import * as echarts from 'echarts'

/**
 * 创建报录比趋势 ECharts 实例
 * @param {HTMLElement} el - 挂载容器
 * @param {Array} stats - PositionStats 数组（需按 year 升序）
 * @param {number} height - 图表高度（默认 200px，用于详情弹窗）
 * @returns {echarts.ECharts} 实例
 */
export function createStatsTrendChart(el, stats, height) {
  if (height) {
    el.style.height = height + 'px'
  }
  const chart = echarts.init(el)
  updateStatsTrendChart(chart, stats)
  return chart
}

/**
 * 更新报录比趋势图数据
 * @param {echarts.ECharts} chart - 已有实例
 * @param {Array} stats - PositionStats 数组
 */
export function updateStatsTrendChart(chart, stats) {
  const years = stats.map((s) => String(s.year))
  chart.setOption(
    {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
      },
      legend: {
        data: ['报名人数', '最低进面分', '最高进面分'],
        bottom: 0,
      },
      grid: {
        left: '5%',
        right: '5%',
        bottom: '14%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: years,
        axisLabel: { formatter: '{value} 年' },
      },
      yAxis: [
        {
          type: 'value',
          name: '报名人数',
          nameTextStyle: { color: '#409EFF' },
          axisLabel: { color: '#409EFF' },
        },
        {
          type: 'value',
          name: '进面分数',
          nameTextStyle: { color: '#E6A23C' },
          axisLabel: { color: '#E6A23C' },
          min: (val) => Math.max(0, Math.floor(val.min * 0.95)),
        },
      ],
      series: [
        {
          name: '报名人数',
          type: 'bar',
          yAxisIndex: 0,
          data: stats.map((s) => s.registrationCount ?? null),
          itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] },
          barMaxWidth: 50,
        },
        {
          name: '最低进面分',
          type: 'line',
          yAxisIndex: 1,
          data: stats.map((s) => s.minEntryScore ?? null),
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { color: '#E6A23C', width: 2 },
          itemStyle: { color: '#E6A23C' },
        },
        {
          name: '最高进面分',
          type: 'line',
          yAxisIndex: 1,
          data: stats.map((s) => s.maxEntryScore ?? null),
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { color: '#F56C6C', width: 2 },
          itemStyle: { color: '#F56C6C' },
        },
      ],
    },
    true,
  )
}
