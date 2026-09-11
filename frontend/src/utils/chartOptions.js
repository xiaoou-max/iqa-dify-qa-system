import { merge } from 'lodash';

export const CHART_COLORS = [
  '#409eff',
  '#67c23a',
  '#e6a23c',
  '#f56c6c',
  '#36cfc9',
  '#9254de',
  '#ff7a45',
  '#2f80ed',
  '#eb2f96',
  '#73d13d',
  '#faad14',
  '#5c7cfa'
];

const toArray = (value) => {
  if (Array.isArray(value)) return value;
  return value ? [value] : [];
};

const getSeriesList = (option) => toArray(option?.series).filter(Boolean);

const getSeriesTypes = (option) => new Set(getSeriesList(option).map((item) => item?.type).filter(Boolean));

const hasSeriesType = (option, type) => getSeriesTypes(option).has(type);

const clampPercentRadius = (radius, maxOuter) => {
  const cap = `${maxOuter}%`;
  if (!radius) return cap;

  const clampOne = (value) => {
    if (typeof value === 'number') return Math.min(value, maxOuter);
    if (typeof value !== 'string') return value;
    const match = value.match(/^(\d+(?:\.\d+)?)%$/);
    if (!match) return value;
    return `${Math.min(Number(match[1]), maxOuter)}%`;
  };

  if (Array.isArray(radius)) {
    const next = [...radius];
    next[1] = clampOne(next[1] || cap);
    return next;
  }

  return clampOne(radius);
};

const normalizeLegend = (legend, layout) => {
  if (Array.isArray(legend)) {
    return legend.map((item) => normalizeLegend(item, layout));
  }

  if (legend?.show === false) return legend;

  const common = {
    type: 'scroll',
    itemWidth: 14,
    itemHeight: 8,
    pageIconColor: '#409eff',
    pageIconInactiveColor: '#cbd5e1',
    pageTextStyle: {
      color: '#64748b'
    },
    textStyle: {
      ...((legend && legend.textStyle) || {}),
      color: '#334155',
      overflow: 'truncate',
      width: layout.mobile ? 82 : 116,
      fontSize: layout.mobile ? 11 : 12
    }
  };

  if (layout.pieLike) {
    return {
      ...(legend || {}),
      ...common,
      orient: 'horizontal',
      left: layout.mobile ? 8 : 16,
      right: layout.mobile ? 8 : 16,
      top: 'auto',
      bottom: layout.mobile ? 8 : 12,
      height: layout.mobile ? 54 : 48,
      width: 'auto',
      itemGap: layout.mobile ? 8 : 12
    };
  }

  return {
    ...(legend || {}),
    ...common,
    orient: 'horizontal',
    left: 'center',
    right: 'auto',
    top: layout.mobile ? 10 : 14,
    bottom: 'auto',
    width: layout.mobile ? '92%' : '82%',
    itemGap: layout.mobile ? 10 : 18
  };
};

const normalizePieSeries = (series, { mobile }) => {
  const pieSeries = series.filter((item) => item?.type === 'pie');
  const pieCount = pieSeries.length;
  const singlePieOuterRadius = mobile ? 48 : 56;
  const multiPieOuterRadius = mobile ? 33 : 40;
  let pieIndex = 0;

  return series.map((item) => {
    if (item?.type !== 'pie') return item;

    const currentIndex = pieIndex;
    pieIndex += 1;
    const isSingle = pieCount <= 1;
    const centerX = isSingle ? '50%' : `${Math.round(((currentIndex + 1) * 100) / (pieCount + 1))}%`;
    const centerY = mobile ? '42%' : '43%';
    const maxRadius = isSingle ? singlePieOuterRadius : multiPieOuterRadius;

    return {
      ...item,
      center: isSingle ? ['50%', centerY] : (item.center || [centerX, centerY]),
      radius: clampPercentRadius(item.radius, maxRadius),
      avoidLabelOverlap: true,
      minShowLabelAngle: item.minShowLabelAngle ?? 4,
      label: {
        show: true,
        color: '#1f2937',
        fontSize: mobile ? 11 : 12,
        overflow: 'break',
        ...item.label
      },
      labelLine: {
        length: mobile ? 8 : 14,
        length2: mobile ? 6 : 12,
        maxSurfaceAngle: 80,
        ...item.labelLine
      },
      emphasis: {
        scale: true,
        scaleSize: 4,
        ...item.emphasis
      }
    };
  });
};

const normalizeSpecialSeries = (series, { mobile }) => series.map((item) => {
  if (item?.type === 'funnel') {
    return {
      ...item,
      top: mobile ? 28 : 34,
      bottom: mobile ? 84 : 76,
      left: mobile ? '8%' : '14%',
      width: mobile ? '84%' : '72%',
      label: {
        color: '#1f2937',
        fontSize: mobile ? 11 : 12,
        ...item.label
      }
    };
  }

  if (item?.type === 'gauge') {
    return {
      ...item,
      center: ['50%', mobile ? '44%' : '46%'],
      radius: mobile ? '66%' : '70%',
      title: {
        color: '#334155',
        fontSize: mobile ? 11 : 12,
        ...item.title
      },
      detail: {
        color: '#0f172a',
        fontSize: mobile ? 18 : 22,
        ...item.detail
      }
    };
  }

  if (item?.type === 'radar') {
    return {
      symbolSize: mobile ? 5 : 6,
      lineStyle: {
        width: 2,
        ...item.lineStyle
      },
      areaStyle: item.areaStyle || {
        opacity: 0.08
      },
      ...item
    };
  }

  return item;
});

const normalizeAxes = (option, { mobile }) => {
  if (option.xAxis) {
    const axes = toArray(option.xAxis).map((axis) => ({
      ...axis,
      axisLabel: {
        interval: 0,
        rotate: mobile ? 40 : 28,
        hideOverlap: true,
        color: '#475569',
        fontSize: mobile ? 11 : 12,
        formatter(value) {
          const text = String(value ?? '');
          return text.length > 18 ? `${text.slice(0, 18)}...` : text;
        },
        ...axis.axisLabel
      },
      axisLine: {
        lineStyle: {
          color: '#dbe3ef'
        },
        ...axis.axisLine
      },
      splitLine: {
        lineStyle: {
          color: '#eef2f7'
        },
        ...axis.splitLine
      }
    }));
    option.xAxis = Array.isArray(option.xAxis) ? axes : axes[0];
  }

  if (option.yAxis) {
    const axes = toArray(option.yAxis).map((axis) => ({
      ...axis,
      axisLabel: {
        color: '#475569',
        fontSize: mobile ? 11 : 12,
        formatter(value) {
          const text = String(value ?? '');
          return text.length > 14 ? `${text.slice(0, 14)}...` : text;
        },
        ...axis.axisLabel
      },
      axisLine: {
        lineStyle: {
          color: '#dbe3ef'
        },
        ...axis.axisLine
      },
      splitLine: {
        lineStyle: {
          color: '#eef2f7'
        },
        ...axis.splitLine
      }
    }));
    option.yAxis = Array.isArray(option.yAxis) ? axes : axes[0];
  }
};

export const parseChartOption = (raw) => {
  if (!raw || typeof raw !== 'string') {
    throw new Error('图表配置为空');
  }

  const cleaned = raw
    .replace(/^```(?:echarts|ech)?\s*/i, '')
    .replace(/```$/i, '')
    .trim();

  return JSON.parse(cleaned);
};

export const isPieLikeChart = (option) => {
  const series = getSeriesList(option);
  return series.some((item) => ['pie', 'funnel', 'gauge', 'radar'].includes(item?.type));
};

export const hasLongAxisLabel = (option) => {
  const xAxisList = Array.isArray(option?.xAxis) ? option.xAxis : [option?.xAxis].filter(Boolean);
  return xAxisList.some((axis) => {
    const data = Array.isArray(axis?.data) ? axis.data : [];
    return data.some((label) => String(label ?? '').length > 10);
  });
};

export const buildChartOption = (option, { mobile = false } = {}) => {
  const pieLike = isPieLikeChart(option);
  const baseOption = {
    animation: false,
    color: CHART_COLORS,
    textStyle: {
      fontFamily: 'inherit',
      fontSize: 13,
      color: '#1f2937'
    },
    title: {
      show: false,
      left: 12,
      top: 10,
      textStyle: {
        color: '#111827',
        fontSize: 16,
        fontWeight: 700
      },
      subtextStyle: {
        color: '#6b7280',
        fontSize: 12
      }
    },
    legend: {
      type: 'scroll',
      top: 14,
      left: 'center',
      right: 'auto',
      width: '82%',
      itemGap: 18,
      itemWidth: 14,
      itemHeight: 8,
      textStyle: {
        color: '#4b5563',
        overflow: 'truncate',
        width: 120
      }
    },
    grid: {
      containLabel: true,
      left: 24,
      right: 24,
      top: 86,
      bottom: mobile ? 84 : 56
    },
    tooltip: {
      trigger: pieLike ? 'item' : 'axis',
      confine: true,
      backgroundColor: 'rgba(17, 24, 39, 0.92)',
      borderWidth: 0,
      textStyle: {
        color: '#fff'
      },
      axisPointer: {
        type: 'shadow'
      }
    },
    toolbox: {
      right: 12,
      bottom: 8,
      feature: {
        saveAsImage: {
          pixelRatio: 2,
          title: '保存图片'
        }
      }
    }
  };

  if (pieLike) {
    baseOption.grid = undefined;
    baseOption.xAxis = undefined;
    baseOption.yAxis = undefined;
  } else {
    baseOption.xAxis = {
      axisLabel: {
        interval: 0,
        rotate: mobile ? 40 : 28,
        hideOverlap: true,
        formatter(value) {
          const text = String(value ?? '');
          return text.length > 18 ? `${text.slice(0, 18)}...` : text;
        }
      }
    };
    baseOption.yAxis = {
      axisLabel: {
        formatter(value) {
          const text = String(value ?? '');
          return text.length > 14 ? `${text.slice(0, 14)}...` : text;
        }
      }
    };
  }

  const finalOption = merge({}, baseOption, option);
  finalOption.title = {
    ...(finalOption.title || {}),
    show: false
  };

  const finalPieLike = isPieLikeChart(finalOption);
  const finalSeriesTypes = getSeriesTypes(finalOption);

  finalOption.legend = normalizeLegend(finalOption.legend, {
    mobile,
    pieLike: finalPieLike
  });

  if (finalPieLike) {
    finalOption.grid = undefined;
    finalOption.xAxis = undefined;
    finalOption.yAxis = undefined;
  } else {
    finalOption.grid = {
      ...((finalOption.grid && !Array.isArray(finalOption.grid)) ? finalOption.grid : {}),
      containLabel: true,
      top: mobile ? 96 : 86,
      left: mobile ? 12 : 24,
      right: mobile ? 12 : 24,
      bottom: mobile ? 86 : 58
    };
    normalizeAxes(finalOption, { mobile });
  }

  if (Array.isArray(finalOption.series)) {
    finalOption.series = normalizeSpecialSeries(
      normalizePieSeries(finalOption.series, { mobile }),
      { mobile }
    );
  } else if (finalOption.series) {
    finalOption.series = normalizeSpecialSeries(
      normalizePieSeries([finalOption.series], { mobile }),
      { mobile }
    )[0];
  }

  if (hasSeriesType(finalOption, 'radar')) {
    const radarOption = Array.isArray(finalOption.radar) ? finalOption.radar[0] : finalOption.radar;
    finalOption.radar = {
      ...(radarOption || {}),
      center: ['50%', mobile ? '39%' : '43%'],
      radius: mobile ? '48%' : '56%',
      axisName: {
        color: '#334155',
        fontSize: mobile ? 11 : 12,
        ...radarOption?.axisName
      },
      splitLine: {
        lineStyle: {
          color: '#dbe3ef'
        },
        ...radarOption?.splitLine
      },
      splitArea: {
        areaStyle: {
          color: ['#ffffff', '#f8fbff']
        },
        ...radarOption?.splitArea
      }
    };
  }

  if (finalSeriesTypes.has('pie')) {
    finalOption.tooltip = {
      ...(finalOption.tooltip || {}),
      trigger: 'item'
    };
  }

  if (finalOption.toolbox?.feature?.dataView) {
    delete finalOption.toolbox.feature.dataView;
  }

  return finalOption;
};
