import json
import uuid
import datetime
import requests
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

DASHSCOPE_EMBEDDING_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings"
DASHSCOPE_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
DASHSCOPE_API_KEY = "YOUR_DASHSCOPE_API_KEY"

TENANT_ID = "36810dd3-e395-4d40-9600-f0c89441e596"
PLUGIN_ID = "langgenius/tongyi"
PLUGIN_UNIQUE_ID = "langgenius/tongyi/tongyi"


def _i18n(en, zh):
    return {"en_US": en, "zh_Hans": zh}


class PluginDaemonHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        try:
            self._handle_request('GET')
        except Exception as e:
            import traceback
            traceback.print_exc()
            self._send_error(500, str(e))

    def do_POST(self):
        try:
            self._handle_request('POST')
        except Exception as e:
            import traceback
            traceback.print_exc()
            self._send_error(500, str(e))

    def _handle_request(self, method):
        parsed = urlparse(self.path)
        path = parsed.path.rstrip('/')
        query = parse_qs(parsed.query)

        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length).decode('utf-8') if content_length > 0 else '{}'

        try:
            request_data = json.loads(body) if body else {}
        except Exception:
            request_data = {}

        # 日志: 记录 LLM 请求是否携带 tools（帮助诊断工具未触发问题）
        if '/dispatch/llm/invoke' in path:
            try:
                req_data = request_data.get('data', request_data)
                has_tools = bool(req_data.get('tools'))
                tool_names = [t.get('name', '') for t in (req_data.get('tools') or []) if isinstance(t, dict)]
                with open('/tmp/daemon_llm.log', 'a', encoding='utf-8') as f:
                    f.write(f"{datetime.datetime.now().isoformat()} model={req_data.get('model')} has_tools={has_tools} tools={tool_names} prompt_len={len(json.dumps(req_data.get('prompt_messages', [])))} last_user={json.dumps((req_data.get('prompt_messages') or [])[-1:], ensure_ascii=False)[:300]}\n")
            except Exception:
                pass

        print(f"[{method}] {path} - data: {body[:200]}", flush=True)

        if '/management/tools/check_existence' in path:
            self._send_json(200, {"code": 0, "message": "", "data": [True]})
        elif '/management/tools' in path:
            self._send_json(200, self._get_tool_providers())
        elif '/management/tool' in path:
            # 单数端点: Dify 通过 GET /management/tool?provider=xxx&plugin_id=yyy
            # 获取单个工具 provider 的 PluginToolProviderEntity
            self._send_json(200, self._get_single_tool_provider(query))
        elif '/management/triggers' in path:
            self._send_json(200, self._get_trigger_providers())
        elif '/management/agent_strategies' in path:
            self._send_json(200, self._get_agent_strategy_providers())
        elif '/management/models' in path:
            self._send_json(200, self._get_model_providers())
        elif '/management/providers' in path:
            self._send_json(200, self._get_management_providers())
        elif '/management/list' in path or '/management/' in path:
            self._send_json(200, self._get_management_list())
        elif '/dispatch/text_embedding/invoke' in path:
            result = self._handle_embedding(request_data)
            self._send_json(200, result)
        elif '/dispatch/llm/invoke' in path:
            self._handle_llm_stream(request_data)
        elif '/dispatch/tool/invoke' in path:
            self._send_json(200, {
                "code": 0, "message": "",
                "data": {"result": "Tool not implemented in plugin daemon"}
            })
        elif '/dispatch/agent_strategy/invoke' in path:
            self._send_json(200, {
                "code": 0, "message": "",
                "data": {"result": "Agent strategy not implemented"}
            })
        elif '/dispatch/model/schema' in path:
            result = self._get_model_schema(request_data)
            self._send_json(200, result)
        elif '/dispatch/model/validate_model_credentials' in path:
            self._send_json(200, self._validate_credentials_response())
        elif '/dispatch/provider/validate_provider_credentials' in path:
            self._send_json(200, self._validate_credentials_response())
        elif '/dispatch/text_embedding/num_tokens' in path:
            self._send_json(200, self._num_tokens_response())
        elif '/dispatch/llm/num_tokens' in path:
            self._send_json(200, self._llm_num_tokens_response())
        elif '/plugins' in path or path.endswith('/plugin'):
            self._send_json(200, {"list": [], "has_more": False})
        elif '/ping' in path or '/health' in path:
            self._send_json(200, {"status": "ok"})
        else:
            self._send_json(200, self._get_model_providers())

    def _send_json(self, status_code, response):
        self.send_response(status_code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        self.wfile.write(json.dumps(response).encode())

    def _send_sse(self, event, data):
        self.wfile.write(f"event: {event}\ndata: {json.dumps(data)}\n\n".encode())

    def _send_ndjson(self, data):
        """Emit a single NDJSON line: {\"code\":0,\"message\":\"\",\"data\":...}"""
        self.wfile.write((json.dumps({"code": 0, "message": "", "data": data}) + "\n").encode())
        self.wfile.flush()

    def _send_ndjson_error(self, message):
        """Emit an NDJSON error line that the Dify api can parse as PluginDaemonError."""
        error_payload = {
            "error_type": "PluginInvokeError",
            "message": message,
        }
        self.wfile.write(
            (
                json.dumps(
                    {"code": -500, "message": json.dumps(error_payload), "data": None}
                )
                + "\n"
            ).encode()
        )
        self.wfile.flush()

    def _send_error(self, status_code, message):
        self.send_response(status_code)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps({"code": -1, "message": message, "data": None}).encode())

    def _get_model_providers(self):
        return {
            "code": 0,
            "message": "",
            "data": [
                {
                    "id": uuid.uuid4().hex,
                    "created_at": datetime.datetime.now().isoformat(),
                    "updated_at": datetime.datetime.now().isoformat(),
                    "provider": "tongyi",
                    "tenant_id": TENANT_ID,
                    "plugin_unique_identifier": PLUGIN_UNIQUE_ID,
                    "plugin_id": PLUGIN_ID,
                    "declaration": {
                        "provider": "tongyi",
                        "provider_name": "tongyi",
                        "label": _i18n("DashScope", "阿里云百炼"),
                        "description": _i18n("阿里云百炼模型服务", "阿里云百炼模型服务"),
                        "icon_small": _i18n(
                            "https://example.com/icon_small.png",
                            "https://example.com/icon_small.png"
                        ),
                        "supported_model_types": ["llm", "text-embedding", "rerank"],
                        "configurate_methods": ["predefined-model"],
                        "provider_credential_schema": {
                            "credential_form_schemas": [
                                {
                                    "type": "secret-input",
                                    "label": _i18n("DashScope API Key", "DashScope API Key"),
                                    "variable": "dashscope_api_key",
                                    "required": True
                                }
                            ]
                        },
                        "model_credential_schema": {
                            "model": {
                                "label": _i18n("Model", "模型")
                            },
                            "credential_form_schemas": []
                        },
                        "models": [
                            {
                                "model": "text-embedding-v3",
                                "label": _i18n("text-embedding-v3", "text-embedding-v3"),
                                "model_type": "text-embedding",
                                "fetch_from": "predefined-model",
                                "model_properties": {},
                                "features": []
                            },
                            {
                                "model": "text-embedding-v2",
                                "label": _i18n("text-embedding-v2", "text-embedding-v2"),
                                "model_type": "text-embedding",
                                "fetch_from": "predefined-model",
                                "model_properties": {},
                                "features": []
                            },
                            {
                                "model": "qwen3.7-plus",
                                "label": _i18n("qwen3.7-plus", "qwen3.7-plus"),
                                "model_type": "llm",
                                "fetch_from": "predefined-model",
                                "model_properties": {},
                                "features": ["tool-call", "multi-tool-call"]
                            },
                            {
                                "model": "qwen3.8-max",
                                "label": _i18n("qwen3.8-max", "qwen3.8-max"),
                                "model_type": "llm",
                                "fetch_from": "predefined-model",
                                "model_properties": {},
                                "features": ["tool-call", "multi-tool-call"]
                            },
                            {
                                "model": "qwen3-rerank",
                                "label": _i18n("qwen3-rerank", "qwen3-rerank"),
                                "model_type": "rerank",
                                "fetch_from": "predefined-model",
                                "model_properties": {},
                                "features": []
                            }
                        ]
                    }
                }
            ]
        }

    def _get_tool_providers(self):
        return {
            "code": 0,
            "message": "",
            "data": [
                {
                    "provider": "tongyi",
                    "plugin_unique_identifier": PLUGIN_UNIQUE_ID,
                    "plugin_id": PLUGIN_ID,
                    "declaration": {
                        "identity": {
                            "author": "langgenius",
                            "name": "tongyi",
                            "description": _i18n("DashScope Tools", "阿里云百炼工具"),
                            "icon": "",
                            "icon_dark": None,
                            "label": _i18n("DashScope", "阿里云百炼"),
                            "tags": []
                        },
                        "plugin_id": PLUGIN_ID,
                        "credentials_schema": [],
                        "oauth_schema": None,
                        "tools": []
                    }
                }
            ]
        }

    def _get_single_tool_provider(self, query):
        """GET /management/tool?provider=xxx&plugin_id=yyy 返回单个 PluginToolProviderEntity"""
        provider = (query.get('provider') or [''])[0]
        plugin_id = (query.get('plugin_id') or [''])[0]

        if provider == 'searxng' or plugin_id == 'langgenius/searxng':
            return self._get_searxng_tool_provider()

        # 兜底返回 tongyi 工具 provider
        data = self._get_tool_providers().get('data', [])
        return {
            "code": 0,
            "message": "",
            "data": data[0] if data else None
        }

    def _get_searxng_tool_provider(self):
        return {
            "code": 0,
            "message": "",
            "data": {
                "provider": "searxng",
                "plugin_unique_identifier": "langgenius/searxng/searxng",
                "plugin_id": "langgenius/searxng",
                "declaration": {
                    "identity": {
                        "author": "langgenius",
                        "name": "searxng",
                        "description": _i18n("SearXNG search engine", "SearXNG 搜索引擎"),
                        "icon": "",
                        "icon_dark": None,
                        "label": _i18n("SearXNG", "SearXNG"),
                        "tags": ["search"]
                    },
                    "plugin_id": "langgenius/searxng",
                    "credentials_schema": [],
                    "oauth_schema": None,
                    "tools": [
                        {
                            "identity": {
                                "author": "langgenius",
                                "name": "searxng_search",
                                "label": _i18n("SearXNG Search", "SearXNG 搜索"),
                                "provider": "searxng",
                                "icon": ""
                            },
                            "description": {
                                "human": _i18n("Search the web using SearXNG", "使用 SearXNG 搜索引擎搜索网页"),
                                "llm": "Search the web using SearXNG meta search engine."
                            },
                            "parameters": [
                                {
                                    "name": "query",
                                    "label": _i18n("Query", "搜索关键词"),
                                    "placeholder": _i18n("Search query", "搜索关键词"),
                                    "human_description": _i18n("The search query", "搜索关键词"),
                                    "type": "string",
                                    "form": "llm",
                                    "required": True,
                                    "llm_description": "The search query",
                                    "options": []
                                },
                                {
                                    "name": "search_type",
                                    "label": _i18n("Search Type", "搜索类型"),
                                    "placeholder": _i18n("Type of search", "搜索类型"),
                                    "human_description": _i18n("Type of search", "搜索类型"),
                                    "type": "select",
                                    "form": "form",
                                    "required": False,
                                    "llm_description": "Type of search: general, videos, images, news",
                                    "options": [
                                        {"value": "general", "label": _i18n("General", "综合")},
                                        {"value": "videos", "label": _i18n("Videos", "视频")},
                                        {"value": "images", "label": _i18n("Images", "图片")},
                                        {"value": "news", "label": _i18n("News", "新闻")}
                                    ]
                                }
                            ],
                            "output_schema": {},
                            "has_runtime_parameters": False
                        }
                    ]
                }
            }
        }

    def _get_trigger_providers(self):
        return {
            "code": 0,
            "message": "",
            "data": [
                {
                    "provider": "tongyi",
                    "plugin_unique_identifier": PLUGIN_UNIQUE_ID,
                    "plugin_id": PLUGIN_ID,
                    "declaration": {
                        "identity": {
                            "author": "langgenius",
                            "name": "tongyi",
                            "label": _i18n("DashScope", "阿里云百炼"),
                            "description": _i18n("DashScope Triggers", "阿里云百炼触发器"),
                            "icon": "",
                            "icon_dark": None,
                            "tags": []
                        },
                        "subscription_schema": [],
                        "subscription_constructor": None,
                        "events": []
                    }
                }
            ]
        }

    def _get_agent_strategy_providers(self):
        return {
            "code": 0,
            "message": "",
            "data": [
                {
                    "provider": "tongyi",
                    "plugin_unique_identifier": PLUGIN_UNIQUE_ID,
                    "plugin_id": PLUGIN_ID,
                    "declaration": {
                        "identity": {
                            "author": "langgenius",
                            "name": "tongyi",
                            "description": _i18n("DashScope Agent Strategies", "阿里云百炼智能体策略"),
                            "icon": "",
                            "icon_dark": None,
                            "label": _i18n("DashScope", "阿里云百炼"),
                            "tags": []
                        },
                        "plugin_id": PLUGIN_ID,
                        "strategies": []
                    },
                    "meta": {
                        "minimum_dify_version": None,
                        "version": "1.0.0"
                    }
                }
            ]
        }

    def _get_management_providers(self):
        return {
            "code": 0,
            "message": "",
            "data": {
                "tongyi": {
                    "provider": "tongyi",
                    "enabled": True
                }
            }
        }

    def _get_management_list(self):
        return {
            "code": 0,
            "message": "",
            "data": {
                "list": [],
                "total": 0,
                "has_more": False
            }
        }

    def _get_model_schema(self, request_data):
        data = request_data.get('data', request_data)
        model = data.get('model', '')
        model_type = data.get('model_type', '')

        return {
            "code": 0,
            "message": "",
            "data": {
                "model_schema": {
                    "model": model,
                    "label": _i18n(model, model),
                    "model_type": model_type,
                    "fetch_from": "predefined-model",
                    "model_properties": {},
                    "features": ["tool-call", "multi-tool-call"] if model_type == "llm" else []
                }
            }
        }

    def _validate_credentials_response(self):
        return {
            "code": 0,
            "message": "",
            "data": {
                "result": True,
                "credentials": {}
            }
        }

    def _num_tokens_response(self):
        return {
            "code": 0,
            "message": "",
            "data": {
                "num_tokens": [0]
            }
        }

    def _llm_num_tokens_response(self):
        return {
            "code": 0,
            "message": "",
            "data": {
                "num_tokens": 0
            }
        }

    def _handle_embedding(self, request_data):
        data = request_data.get('data', request_data)
        texts = data.get('texts', [])
        model = data.get('model', 'text-embedding-v2')

        if not DASHSCOPE_API_KEY:
            return {"code": 1, "message": "No API key provided", "data": None}

        try:
            url = DASHSCOPE_EMBEDDING_URL
            headers = {
                'Authorization': f'Bearer {DASHSCOPE_API_KEY}',
                'Content-Type': 'application/json'
            }
            payload = {
                'model': model,
                'input': texts
            }

            response = requests.post(url, headers=headers, json=payload, timeout=30)

            if response.status_code == 200:
                result = response.json()
                data_list = result.get('data', [])
                vectors = [e.get('embedding', []) for e in data_list]

                usage = result.get('usage', {})
                total_tokens = usage.get('total_tokens', 0)

                # Dify 的 EmbeddingResult 期望字段: model / embeddings / usage(tokens,total_tokens,
                # unit_price,price_unit,total_price,currency,latency)，若字段不全会导致 Pydantic 校验失败
                return {
                    "code": 0,
                    "message": "",
                    "data": {
                        "model": model,
                        "embeddings": vectors,
                        "usage": {
                            "tokens": total_tokens,
                            "total_tokens": total_tokens,
                            "unit_price": 0,
                            "price_unit": 0,
                            "total_price": 0,
                            "currency": "CNY",
                            "latency": 0.0
                        }
                    }
                }
            else:
                print(f"Embedding API error: {response.status_code} - {response.text[:500]}")
                return {"code": 1, "message": f"API error: {response.status_code}", "data": None}
        except Exception as e:
            print(f"Embedding exception: {str(e)}")
            return {"code": 1, "message": str(e), "data": None}

    def _handle_llm_stream(self, request_data):
        self.close_connection = True
        data = request_data.get('data', request_data)
        model = data.get('model', 'qwen3.7-plus')
        prompt_messages = data.get('prompt_messages', [])
        model_parameters = data.get('model_parameters', {})
        credentials = data.get('credentials', {})

        api_key = DASHSCOPE_API_KEY

        if not api_key:
            self._send_error(400, "No API key provided")
            return

        messages = []
        for msg in prompt_messages:
            if isinstance(msg, dict):
                role = msg.get('role', 'user')
                content = msg.get('content', '')
                if isinstance(content, list):
                    text_content = ''
                    for c in content:
                        if isinstance(c, dict) and c.get('type') == 'text':
                            text_content += c.get('text', '')
                    content = text_content
                messages.append({"role": role, "content": content})

        if not messages or messages[-1].get('role') != 'user':
            messages.append({"role": "user", "content": "Hello"})

        url = f"{DASHSCOPE_API_URL}/chat/completions"
        headers = {
            'Authorization': f'Bearer {api_key}',
            'Content-Type': 'application/json'
        }

        temperature = model_parameters.get('temperature', 0.7)
        max_tokens = model_parameters.get('max_tokens', 2000)

        payload = {
            'model': model,
            'messages': messages,
            'stream': True,
            'temperature': temperature,
            'max_tokens': max_tokens
        }

        # 将 Dify 传入的工具定义转换为 DashScope OpenAI 兼容格式，触发原生函数调用
        tools = data.get('tools', []) or []
        dashscope_tools = []
        for t in tools:
            if isinstance(t, dict):
                dashscope_tools.append({
                    "type": "function",
                    "function": {
                        "name": t.get("name", ""),
                        "description": t.get("description", ""),
                        "parameters": t.get("parameters", {}) or {}
                    }
                })
        if dashscope_tools:
            payload["tools"] = dashscope_tools

        self.send_response(200)
        self.send_header('Content-Type', 'application/x-ndjson')
        self.send_header('Cache-Control', 'no-cache')
        self.send_header('Connection', 'close')
        self.end_headers()

        try:
            response = requests.post(url, headers=headers, json=payload, stream=True, timeout=120)

            if response.status_code != 200:
                error_data = {
                    "code": -500,
                    "message": f"API error: {response.status_code}"
                }
                self._send_ndjson_error(f"API error: {response.status_code}")
                return

            full_content = ""
            chunk_index = 0
            # 累积 DashScope 流式返回的原生工具调用参数
            tool_calls_acc = {}  # index -> {"id":..., "name":..., "arguments":...}
            has_tool_calls = False

            def _emit_tool_calls():
                """将累积的工具调用转换为 Dify LLMResultChunk 期望的 delta.message.tool_calls 格式"""
                tool_calls = []
                for idx in sorted(tool_calls_acc.keys()):
                    acc = tool_calls_acc[idx]
                    tool_calls.append({
                        "id": acc["id"],
                        "type": "function",
                        "function": {
                            "name": acc["name"],
                            "arguments": acc["arguments"]
                        }
                    })
                chunk = {
                    "model": model,
                    "prompt_messages": [],
                    "system_fingerprint": None,
                    "delta": {
                        "index": chunk_index,
                        "message": {
                            "role": "assistant",
                            "content": "",
                            "tool_calls": tool_calls
                        },
                        "usage": None,
                        "finish_reason": "tool_calls"
                    }
                }
                self._send_ndjson(chunk)

            for line in response.iter_lines():
                if not line:
                    continue
                line = line.decode('utf-8')

                if line.startswith('data: '):
                    data_str = line[6:]
                    if data_str == '[DONE]':
                        # 有原生工具调用时先发送 tool_calls 块
                        if has_tool_calls:
                            _emit_tool_calls()
                        # 关键修复：Dify 端会把每个 chunk 的 delta.message.content 依次拼接，
                        # 结尾 chunk 必须为空 content，否则完整内容会被重复拼接导致 JSON 重复
                        finish_chunk = {
                            "model": model,
                            "prompt_messages": [],
                            "system_fingerprint": None,
                            "delta": {
                                "index": chunk_index,
                                "message": {
                                    "role": "assistant",
                                    "content": "",
                                    "usage": {
                                        "prompt_tokens": 0,
                                        "completion_tokens": 0,
                                        "total_tokens": 0
                                    }
                                },
                                "usage": None,
                                "finish_reason": "stop"
                            }
                        }
                        self._send_ndjson(finish_chunk)
                        return

                    try:
                        chunk = json.loads(data_str)
                        choices = chunk.get('choices', [])
                        if choices:
                            delta = choices[0].get('delta', {})
                            content = delta.get('content', '')
                            if content:
                                full_content += content
                                result_chunk = {
                                    "model": model,
                                    "prompt_messages": [],
                                    "system_fingerprint": None,
                                    "delta": {
                                        "index": chunk_index,
                                        "message": {
                                            "role": "assistant",
                                            "content": content
                                        },
                                        "usage": None,
                                        "finish_reason": None
                                    }
                                }
                                self._send_ndjson(result_chunk)
                                chunk_index += 1

                            # 处理原生工具调用（参数分片累积）
                            delta_tool_calls = delta.get('tool_calls')
                            if delta_tool_calls:
                                has_tool_calls = True
                                for tc in delta_tool_calls:
                                    tc_index = tc.get('index', 0)
                                    if tc_index not in tool_calls_acc:
                                        tool_calls_acc[tc_index] = {
                                            "id": tc.get('id') or f"call_{tc_index}",
                                            "name": "",
                                            "arguments": ""
                                        }
                                    fn = tc.get('function', {})
                                    if fn.get('name'):
                                        tool_calls_acc[tc_index]["name"] = fn.get('name', '')
                                    if fn.get('arguments'):
                                        tool_calls_acc[tc_index]["arguments"] += fn.get('arguments', '')
                    except json.JSONDecodeError:
                        pass

        except Exception as e:
            print(f"LLM stream error: {str(e)}")
            try:
                error_chunk = {
                    "model": model,
                    "prompt_messages": [],
                    "system_fingerprint": None,
                    "delta": {
                        "index": chunk_index if 'chunk_index' in dir() else 0,
                        "message": {
                            "role": "assistant",
                            "content": f"Error: {str(e)}"
                        },
                        "usage": None,
                        "finish_reason": "error"
                    }
                }
                self._send_ndjson(error_chunk)
            except Exception:
                pass

    def log_message(self, format, *args):
        print(f"[Plugin Daemon] {args}")


if __name__ == '__main__':
    server = HTTPServer(('0.0.0.0', 5002), PluginDaemonHandler)
    print('Plugin Daemon v3 running on port 5002...')
    print('Ready to handle Dify plugin requests')
    server.serve_forever()
