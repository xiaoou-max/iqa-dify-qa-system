"""
Query Database Ly (洛阳市房地产数据查询 HTTP 接口)
-------------------------------------------------------
匹配原始系统 http://yth.bit-service.com:30770/adi/bitapi/query_database_ly 的契约：

POST /adi/bitapi/query_database_ly
Body (JSON):
{
  "bits_sql": [
      {"sql": "SELECT ...", "queryContent": "市本级期房销售额"}
  ],
  "sqls": "...原始sql分号分隔...",
  "queryContents": "...查询内容分号分隔...",
  "question": "市本级期房最近5年销售走势？",
  "write_excel": 1        // 1 = 生成 Excel 下载链接, 0 = 不生成
}

Response (JSON，Dify「获取查询结果」子工作流解析契约):
{
  "type": "success" | "error",
  "error_msg": "...仅当 type=error",
  "result": {                   // 每条 SQL 的执行结果
    "result_0": {
        "data": [{ "年份": "2021", ... }],
        "queryContent": "xxx"
    }, ...
  },
  "excel_url": "https://.../downloads/xxx.xlsx",  // write_excel=1 且有数据
  "output_echarts": [                              // 每条 SQL 对应一个图表配置
    {
       "chartType": "bar|line|pie|none",
       "chartsFullTitle": "销售走势",
       "xAxisColumns": ["年份"],
       "seriesColumns": [{"columnName": "销售额_亿元", "chartsTitle": "销售额(亿元)"}],
       "echartsOption": { ... 标准 ECharts option ... }
    }, ...
  ]
}
"""

import os
import io
import re
import json
import uuid
import math
import time
import base64
import hashlib
import threading
from datetime import datetime
from typing import Any

import httpx
import openpyxl
from fastapi import FastAPI, Request, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, FileResponse

# ---------- 配置 ----------
CLICKHOUSE_HOST = os.getenv("CH_HOST", "host.docker.internal")
CLICKHOUSE_PORT = int(os.getenv("CH_PORT", "18123"))
CLICKHOUSE_USER = os.getenv("CH_USER", "default")
CLICKHOUSE_PASS = os.getenv("CH_PASS", "ChLy2026!Qaz")
DOWNLOAD_DIR = os.getenv("DOWNLOAD_DIR", "/downloads")
BASE_URL      = os.getenv("BASE_URL", "http://localhost:30770")
PORT          = int(os.getenv("PORT", "30770"))

os.makedirs(DOWNLOAD_DIR, exist_ok=True)

AUTH_B64 = base64.b64encode(f"{CLICKHOUSE_USER}:{CLICKHOUSE_PASS}".encode()).decode()
CH_URL = f"http://{CLICKHOUSE_HOST}:{CLICKHOUSE_PORT}/"
# ClickHouse 24.x 不允许同时传 Authorization + X-ClickHouse-* 头，只用 Basic Auth 即可
CH_HEADERS = {
    "Authorization": f"Basic {AUTH_B64}",
}

app = FastAPI(title="Query DB Ly")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------- ClickHouse 执行 ----------
async def execute_sql(sql: str) -> list[dict]:
    """执行 SQL 并返回 JSON Array (FORMAT JSON -> list of dict rows)"""
    # 归一化 LLM 工具调用参数中可能残留的反斜杠转义引号：\" -> "
    sql = sql.replace('\\"', '"')
    sql_clean = sql.rstrip().rstrip(";").strip()
    if not sql_clean:
        return []
    # 为所有未加引号的中文标识符补充双引号（AS 别名、ORDER BY、GROUP BY、WHERE 列引用等）
    # 规则：跳过 'string' 和 "already_quoted" 内部；遇到独立中文标识符 token 则包裹双引号
    sql_clean = _quote_cn_identifiers(sql_clean)
    sql_final = f"{sql_clean} FORMAT JSON"
    async with httpx.AsyncClient(timeout=120) as client:
        r = await client.post(CH_URL, content=sql_final.encode("utf-8"), headers=CH_HEADERS)
        if r.status_code != 200:
            raise RuntimeError(f"ClickHouse Err {r.status_code}: {r.text[:300]}")
        data = r.json()
        return data.get("data", [])


def _quote_cn_identifiers(sql: str) -> str:
    """Scan SQL char-by-char; skip content inside '...' or "..." ; wrap non-quoted CN tokens with "" """
    CN_RE = re.compile(r'[\u4e00-\u9fa5][\u4e00-\u9fa5A-Za-z0-9_]*')
    out: list[str] = []
    i = 0
    n = len(sql)
    in_sq = False  # '
    in_dq = False  # "
    while i < n:
        ch = sql[i]
        if in_sq:
            out.append(ch)
            if ch == "'" and (i + 1 < n and sql[i + 1] == "'"):
                out.append(sql[i + 1]); i += 2; continue
            if ch == "'":
                in_sq = False
            i += 1; continue
        if in_dq:
            out.append(ch)
            if ch == '"':
                in_dq = False
            i += 1; continue
        if ch == "'":
            in_sq = True; out.append(ch); i += 1; continue
        if ch == '"':
            in_dq = True; out.append(ch); i += 1; continue
        # Attempt CN identifier match here
        m = CN_RE.match(sql, i)
        if m:
            token = m.group(0)
            # ensure boundary: char before shouldn't be part of identifier
            before = sql[i - 1] if i > 0 else ''
            after = sql[m.end()] if m.end() < n else ''
            is_boundary_before = (i == 0) or (not (before.isalnum() or before == '_' or '\u4e00' <= before <= '\u9fa5'))
            is_boundary_after  = (m.end() == n) or (not (after.isalnum() or after == '_' or '\u4e00' <= after <= '\u9fa5'))
            if is_boundary_before and is_boundary_after:
                out.append(f'"{token}"')
                i = m.end(); continue
        out.append(ch); i += 1
    return ''.join(out)


# ---------- Excel 生成 ----------
def build_excel(result_map: dict) -> str:
    """
    result_map: { "result_0": {"data": [...], "queryContent": str}, ... }
    返回下载相对 URL
    """
    wb = openpyxl.Workbook()
    first = True
    for key, val in result_map.items():
        rows = val.get("data") or []
        title = (val.get("queryContent") or key)[:30] or key
        ws = wb.active if first else wb.create_sheet(title=title)
        first = False
        if rows:
            headers = list(rows[0].keys())
            ws.append(headers)
            for r in rows:
                ws.append([r.get(h) for h in headers])
    fname = f"洛阳房产销售数据-{datetime.now().strftime('%Y%m%d-%H%M%S')}-{uuid.uuid4().hex[:6]}.xlsx"
    fpath = os.path.join(DOWNLOAD_DIR, fname)
    wb.save(fpath)
    return f"{BASE_URL}/downloads/{fname}"


# ---------- 图表类型判断 + ECharts option 生成 ----------
def analyze_chart(data: list[dict], question: str, query_content: str):
    """
    返回 (chartType, xAxisColumns, seriesColumns, chartsFullTitle, echartsOption)
    """
    if not data or len(data) <= 1:
        return {"chartType": "none", "chartsFullTitle": "", "xAxisColumns": [], "seriesColumns": [], "echartsOption": None}
    if len(data) > 30:  # 超过 30 条不建议画图
        return {"chartType": "none", "chartsFullTitle": "", "xAxisColumns": [], "seriesColumns": [], "echartsOption": None}

    obj0 = data[0]
    # ---- 分类 key 列（非数值字符串列） vs 数值列 ----
    all_keys = list(obj0.keys())

    def _is_num(v) -> bool:
        if isinstance(v, bool): return False
        if isinstance(v, (int, float)): return True
        if v is None: return False
        try:
            float(v); return True
        except Exception:
            return False

    # 分类列：字符串且不能被解析为纯数字（"2021" 虽是数字字符串但语义是年份，仍为分类列）
    # 实现方式：如果是 str、且不是数字、或者是"数字年份类短字符串+其它列同为str"的情况 -> 更简单的分法：
    #   num_cols 要求值可以转为 float 且 > 50% 的行是数字（排除列值本身是数字字符串的类别列如 '2021','2022'？No——年份作为 xAxis 合适，不作为数值 series）
    # 所以策略：
    #   - 先把所有列按"能否解析为数字"分出 candidate_num
    #   - 若某列值可以解析为数字但该列 N 行有 90%+ 是数字 -> 候选数值列
    #   - 在候选数值列中，剔除"明显的年份/月份/季度分类列"：取值集合 <= 15 个 且 全为 2-4 位整数 且 值在 1900-2100 或 1-12 或 1-4
    def col_stat(col):
        vals = [row.get(col) for row in data if row.get(col) is not None and row.get(col) != ""]
        num_count = sum(1 for v in vals if _is_num(v))
        return vals, num_count, len(vals)

    candidate_num = []
    candidate_cat = []
    for c in all_keys:
        vals, num_count, tot = col_stat(c)
        if tot > 0 and num_count / tot >= 0.8:  # 80% 为数值
            # 判断是否为"时间/类别型数字"而非可加总的度量
            unique_f = []
            try:
                for v in vals:
                    fv = float(v)
                    if fv.is_integer() and fv not in unique_f:
                        unique_f.append(fv)
            except Exception:
                pass
            is_cat_num = False
            if 1 < len(unique_f) <= 12:  # 分类数少
                all_int = all(abs(x - round(x)) < 1e-9 for x in unique_f)
                if all_int:
                    rng = (min(unique_f), max(unique_f))
                    if (1900 <= rng[0] <= 2100 and 1900 <= rng[1] <= 2100):  # 年份
                        is_cat_num = True
                    elif (1 <= rng[0] <= 12 and 1 <= rng[1] <= 12):  # 月份
                        is_cat_num = True
                    elif (1 <= rng[0] <= 4 and 1 <= rng[1] <= 4):  # 季度
                        is_cat_num = True
            if is_cat_num:
                candidate_cat.append(c)
            else:
                candidate_num.append(c)
        else:
            candidate_cat.append(c)

    # 如果候选数值列为空：再从 candidate_cat 里尝试选最多 1 列"看起来是数值"的
    if not candidate_num:
        # 从 cat 中捞 80%+ 可解析为数值且数值较大的列
        for c in list(candidate_cat):
            vals, nc, tot = col_stat(c)
            if tot > 0 and nc / tot >= 0.8:
                candidate_num.append(c)
        candidate_cat = [c for c in candidate_cat if c not in candidate_num]

    num_cols = candidate_num
    str_cols = candidate_cat

    if not num_cols:
        return {"chartType": "none", "chartsFullTitle": "", "xAxisColumns": [], "seriesColumns": [], "echartsOption": None}

    # 强制规则：优先使用本条 SQL 自身的 queryContent 判断（避免 question 中其他 SQL 的关键词污染）
    self_kw = (query_content or "")
    global_kw = (question or "")
    def has_any(s, words): return any(w in s for w in words)
    self_pie = has_any(self_kw, ["饼图", "占比", "比例", "构成"])
    self_line = has_any(self_kw, ["走势", "趋势", "折线", "变化"])
    self_bar = has_any(self_kw, ["柱状", "对比", "排名"])

    global_pie = has_any(global_kw, ["饼图", "占比", "比例", "构成"])
    global_line = has_any(global_kw, ["走势", "趋势", "折线", "变化"])
    global_bar = has_any(global_kw, ["柱状", "对比", "排名"])

    force_pie  = self_pie  or (not (self_line or self_bar) and global_pie)
    force_line = self_line or (not (self_pie  or self_bar) and global_line)
    force_bar  = self_bar  or (not (self_pie  or self_line) and global_bar)

    def is_date_cat(col: str) -> bool:
        vals = [str(row.get(col, "")) for row in data[:5] if row.get(col) is not None]
        if not vals: return False
        ok_cnt = sum(1 for v in vals if re.match(r"^\d{4}(-\d{1,2}(-\d{1,2})?)?$|^\d{4}年$|^\d{1,2}月$|^Q[1-4]$", v))
        return ok_cnt / max(len(vals), 1) >= 0.6

    has_date_x = any(is_date_cat(c) for c in str_cols) if str_cols else False

    # 冲突裁决
    # 优先级：self_* > 数据形状推断
    if not (force_pie or force_line or force_bar):
        # 根据形状推断
        if has_date_x and len(data) >= 3:
            force_line = True
        elif len(str_cols) >= 1 and len(data) <= 10 and len(num_cols) == 1:
            # 单一指标多分类 -> pie 合理
            if not str_cols or len(set("".join(str(row.get(c,"")) for c in str_cols) for row in data)) == len(data):
                force_pie = True
            else:
                force_bar = True
        else:
            force_bar = True

    # 最终定型
    if force_line:
        chartType = "line"
    elif force_pie:
        chartType = "pie"
    else:
        chartType = "bar"

    # 维度列选择：
    if chartType == "pie":
        # 只取第一个非数值分类列作为饼图 label（避免拼接多列的冗长）
        if str_cols:
            nameCols = [str_cols[0]]
        else:
            return {"chartType": "none", "chartsFullTitle": "", "xAxisColumns": [], "seriesColumns": [], "echartsOption": None}
        # 数值列：只取第一列数值（饼图只能有 1 个度量）
        seriesCols = [{"columnName": num_cols[0], "chartsTitle": _brief_title(num_cols[0], query_content)}]
        full_title = _make_title(seriesCols, query_content or "占比")
        option = _pie_option(data, nameCols, num_cols[0], full_title)
        return {
            "chartType": "pie", "chartsFullTitle": full_title,
            "xAxisColumns": [], "seriesColumns": seriesCols, "echartsOption": option,
        }
    else:  # bar / line
        xCols = list(dict.fromkeys(str_cols)) if str_cols else []
        seriesCols = [{"columnName": c, "chartsTitle": _brief_title(c, query_content)} for c in num_cols]
        default_suffix = "销售趋势" if chartType == "line" else "销售统计"
        full_title = _make_title(seriesCols, (query_content or default_suffix))
        if not xCols:
            return {"chartType": "none", "chartsFullTitle": "", "xAxisColumns": [], "seriesColumns": [], "echartsOption": None}
        option = _cartesian_option(data, xCols, num_cols, full_title, chartType)
        return {
            "chartType": chartType, "chartsFullTitle": full_title,
            "xAxisColumns": xCols, "seriesColumns": seriesCols, "echartsOption": option,
        }


def _brief_title(col: str, query_content: str) -> str:
    """为 series 的展示名：如果 query_content 有语义且不包含 col 名，叠加 query_content 前缀（避免过短重复）"""
    suffix = col
    qc = (query_content or "").strip()
    if qc:
        # 去掉末尾的常见冗余词
        prefix = re.sub(r"(的)?(情况|统计|明细|数据|销售|走势|占比|对比|分析|报表)+$", "", qc).strip()
        prefix = re.sub(r"^(.+?)各(年|季度|月|区县|房类|地区).*$", r"\1", prefix).strip() if "各" in prefix else prefix
        if prefix and len(prefix) <= 10 and prefix not in col:
            return f"{prefix}-{col}"
    return suffix


def _make_title(seriesCols, default):
    names = [s["chartsTitle"] for s in seriesCols]
    if not names: return (default or "")[:20]
    n = "、".join(names)
    if len(n) > 18: n = n[:17] + "…"
    return n if n else (default or "")[:20]

def _clean(v):
    try:
        if isinstance(v, bool): return v
        if isinstance(v, (int, float)): return v
        if v is None: return 0
        return float(v)
    except Exception:
        return None

def _cartesian_option(data, xCols, numCols, title, ctype):
    cats = []
    for r in data:
        cats.append(" / ".join([str(r.get(c, "")) for c in xCols]) if xCols else "")
    series = []
    for nc in numCols:
        series.append({
            "name": nc,
            "type": ctype,
            "data": [_clean(r.get(nc)) for r in data],
            "barMaxWidth": 50,
            "smooth": True,
            "label": {"show": len(data) <= 8, "position": "top"},
        })
    legend_data = [s["name"] for s in series]
    return {
        "title": {"text": title, "left": "center", "textStyle": {"fontSize": 16}},
        "tooltip": {"trigger": "axis"},
        "legend": {"data": legend_data, "top": 30},
        "grid": {"top": 70, "bottom": 60, "left": 60, "right": 40},
        "xAxis": {"type": "category", "data": cats, "axisLabel": {"rotate": 30 if len(cats) > 5 else 0, "interval": 0}},
        "yAxis": {"type": "value"},
        "series": series,
    }

def _pie_option(data, nameCols, numCol, title):
    pdata = []
    for r in data:
        name = " / ".join([str(r.get(c, "")) for c in nameCols])
        val = _clean(r.get(numCol))
        if val is None: continue
        pdata.append({"name": name, "value": val})
    return {
        "title": {"text": title, "left": "center", "textStyle": {"fontSize": 16}},
        "tooltip": {"trigger": "item", "formatter": "{b}: {c} ({d}%)"},
        "legend": {"orient": "vertical", "left": "left", "top": "middle"},
        "series": [{
            "name": numCol,
            "type": "pie",
            "radius": ["40%", "70%"],
            "center": ["60%", "55%"],
            "data": pdata,
            "label": {"formatter": "{b}\n{d}%"}
        }]
    }


# ---------- 数据简化（数据>20条时缩减） ----------
def reduce_data(original: list[dict]) -> list[dict]:
    if not original or len(original) <= 20:
        return original
    first_keys = list(original[0].keys())
    num_cols = []
    for k in first_keys:
        try:
            float(original[0][k]); num_cols.append(k)
        except Exception: pass
    total = len(original)
    step = math.ceil(total / 20)
    reduced = original[::step]
    if len(reduced) >= 2:
        last = original[-1]
        if last is not reduced[-1]:
            reduced[-1] = last
    # 如果有数值列，保留总数据量作为元信息（在第一条追加隐藏字段不现实，这里只返回抽样）
    # 返回 reduced；调用方负责把总条数告诉前端
    return reduced


# ---------- API ----------
@app.post("/adi/bitapi/query_database_ly")
async def query_db(request: Request):
    try:
        payload = await request.json()
    except Exception:
        raise HTTPException(400, "Body 必须是 JSON")
    bits_sql   = payload.get("bits_sql") or []          # 优先使用
    raw_sqls   = payload.get("sqls", "")                # 分号分隔
    qcs        = payload.get("queryContents", "")
    question   = payload.get("question", "")
    write_excel= int(payload.get("write_excel", 0))

    # 构造统一结构
    if bits_sql:
        statements = [(it.get("sql", ""), it.get("queryContent", "")) for it in bits_sql]
    else:
        sl = [s.strip() for s in raw_sqls.split(";") if s.strip()]
        ql = [s.strip() for s in qcs.split(";") if s.strip()]
        statements = []
        for i, s in enumerate(sl):
            statements.append((s, ql[i] if i < len(ql) else f"查询{i+1}"))

    if not statements:
        return JSONResponse({
            "type": "error",
            "error_msg": "没有需要执行的 SQL 语句"
        })

    result_map = {}      # "result_i" -> {"data": [...], "queryContent": "..."}
    echarts_list = []
    errors = []
    original_datas = {}  # 用于 Excel

    for idx, (sql, qc) in enumerate(statements):
        try:
            rows = await execute_sql(sql)
            original_datas[f"result_{idx}"] = {"data": rows, "queryContent": qc}
            show_rows = reduce_data(rows)
            result_map[f"result_{idx}"] = {"data": show_rows, "queryContent": qc}
            # 图表分析
            chart_info = analyze_chart(rows, question, qc)
            echarts_list.append(chart_info)
        except Exception as e:
            errors.append(f"SQL{idx+1} 执行失败: {e}")
            result_map[f"result_{idx}"] = {"data": [], "queryContent": qc}
            echarts_list.append({"chartType": "none", "chartsFullTitle": "", "xAxisColumns": [], "seriesColumns": [], "echartsOption": None})

    if errors and not any(v["data"] for v in result_map.values()):
        return JSONResponse({"type": "error", "error_msg": "；".join(errors)})

    resp = {"type": "success", "result": result_map, "output_echarts": echarts_list}

    if write_excel == 1:
        # 检查是否至少一条数据
        has_any = any(v["data"] for v in original_datas.values())
        if has_any:
            try:
                resp["excel_url"] = build_excel(original_datas)
            except Exception as e:
                resp["excel_url"] = ""

    if errors:
        resp["warning"] = errors

    return JSONResponse(resp)


@app.get("/downloads/{fname}")
async def download(fname: str):
    p = os.path.join(DOWNLOAD_DIR, fname)
    if not os.path.exists(p):
        raise HTTPException(404, "文件不存在")
    return FileResponse(p, filename=fname, media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")


@app.get("/health")
async def health():
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            r = await client.post(CH_URL, content="SELECT 1", headers=CH_HEADERS)
            return {"ok": r.status_code == 200, "ch": r.text.strip()}
    except Exception as e:
        return {"ok": False, "err": str(e)}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=PORT)
