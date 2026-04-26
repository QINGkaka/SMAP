import React from 'react'

type StatusTone = 'success' | 'warning' | 'error' | 'neutral' | 'info'

type SidebarItem = {
  key: string
  label: string
  active?: boolean
  badge?: string
}

type KpiCard = {
  label: string
  value: string
  delta: string
  tone: StatusTone
}

type ProjectSummary = {
  name: string
  owner: string
  updatedAt: string
  repository: string
  branch: string
  status: string
  statusTone: StatusTone
}

type TaskSummary = {
  label: string
  value: string
  helper: string
}

type UploadItem = {
  name: string
  type: string
  updatedAt: string
  size: string
  status: string
  tone: StatusTone
  progress: number
}

type TaskRow = {
  name: string
  owner: string
  status: string
  tone: StatusTone
  startedAt: string
  duration: string
}

type ThresholdField = {
  key: string
  label: string
  low: string
  medium: string
  high: string
}

const sidebarItems: SidebarItem[] = [
  { key: 'projects', label: '项目管理', active: true },
  { key: 'quality', label: '质量指标' },
  { key: 'uploads', label: '文件管理' },
  { key: 'tasks', label: '任务记录', badge: '12' },
  { key: 'reports', label: '报告中心' },
  { key: 'settings', label: '阈值配置' }
]

const kpiCards: KpiCard[] = [
  { label: '当前项目', value: '12', delta: '+2 本周', tone: 'info' },
  { label: '待处理任务', value: '5', delta: '2 个高优先级', tone: 'warning' },
  { label: '上传文件', value: '86', delta: '最新同步 9 分钟前', tone: 'neutral' },
  { label: '高风险指标', value: '3', delta: '较昨日 -1', tone: 'success' }
]

const projectSummary: ProjectSummary = {
  name: 'Payment Risk Engine',
  owner: '平台研发组',
  updatedAt: '2026-04-26 09:40',
  repository: 'git@repo.company.local:risk/payment-engine.git',
  branch: 'release/2026.04',
  status: '运行中',
  statusTone: 'success'
}

const taskSummaries: TaskSummary[] = [
  { label: 'LoC', value: '18.4k', helper: '最近一次 2 分钟前' },
  { label: '复杂度', value: '19 高风险方法', helper: '待复核 4 项' },
  { label: '模型文件', value: '14 个 UML 文件', helper: '异常 3 项' }
]

const uploadItems: UploadItem[] = [
  {
    name: 'payment-core.zip',
    type: 'ZIP',
    updatedAt: '2026-04-26 09:36',
    size: '18.2 MB',
    status: '已完成',
    tone: 'success',
    progress: 100
  },
  {
    name: 'gateway-service.java',
    type: 'JAVA',
    updatedAt: '2026-04-26 09:32',
    size: '428 KB',
    status: '分析中',
    tone: 'warning',
    progress: 72
  },
  {
    name: 'payment-domain.xmi',
    type: 'XMI',
    updatedAt: '2026-04-26 09:20',
    size: '1.2 MB',
    status: '上传失败',
    tone: 'error',
    progress: 38
  }
]

const taskRows: TaskRow[] = [
  {
    name: '主分支质量扫描',
    owner: '陈晨',
    status: '已完成',
    tone: 'success',
    startedAt: '2026-04-26 09:10',
    duration: '03:12'
  },
  {
    name: '复杂度重算',
    owner: '赵敏',
    status: '执行中',
    tone: 'warning',
    startedAt: '2026-04-26 09:22',
    duration: '01:48'
  },
  {
    name: '模型文件校验',
    owner: '系统',
    status: '失败',
    tone: 'error',
    startedAt: '2026-04-26 08:56',
    duration: '00:52'
  },
  {
    name: '综合报告导出',
    owner: '王磊',
    status: '排队中',
    tone: 'neutral',
    startedAt: '2026-04-26 09:38',
    duration: '--'
  }
]

const thresholdFields: ThresholdField[] = [
  { key: 'wmc', label: 'WMC', low: '10', medium: '20', high: '30' },
  { key: 'cbo', label: 'CBO', low: '8', medium: '14', high: '20' },
  { key: 'dit', label: 'DIT', low: '3', medium: '5', high: '7' },
  { key: 'lcom', label: 'LCOM', low: '12', medium: '20', high: '28' },
  { key: 'commentRate', label: '注释率', low: '0.15', medium: '0.1', high: '0.05' },
  { key: 'cyclomatic', label: '圈复杂度', low: '8', medium: '12', high: '18' }
]

const toneClassMap: Record<StatusTone, string> = {
  success: 'bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-200',
  warning: 'bg-amber-50 text-amber-700 ring-1 ring-inset ring-amber-200',
  error: 'bg-rose-50 text-rose-700 ring-1 ring-inset ring-rose-200',
  neutral: 'bg-slate-100 text-slate-700 ring-1 ring-inset ring-slate-200',
  info: 'bg-blue-50 text-blue-700 ring-1 ring-inset ring-blue-200'
}

function StatusBadge({ tone, children }: { tone: StatusTone; children: React.ReactNode }) {
  return (
    <span
      className={[
        'inline-flex h-6 items-center rounded-md px-2.5 text-xs font-medium',
        toneClassMap[tone]
      ].join(' ')}
    >
      {children}
    </span>
  )
}

function SidebarIcon() {
  return (
    <span className="inline-flex h-4 w-4 items-center justify-center rounded-sm bg-slate-300/70">
      <span className="h-1.5 w-1.5 rounded-[2px] bg-slate-600" />
    </span>
  )
}

function SectionTitle({
  title,
  description,
  action
}: {
  title: string
  description?: string
  action?: React.ReactNode
}) {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-6 py-4">
      <div className="space-y-1">
        <h2 className="text-base font-semibold text-slate-900">{title}</h2>
        {description ? <p className="text-sm text-slate-500">{description}</p> : null}
      </div>
      {action}
    </div>
  )
}

export default function ProjectManagementDashboard() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <div className="grid min-h-screen grid-cols-[220px_minmax(0,1fr)]">
        <aside className="border-r border-slate-200 bg-white">
          <div className="flex h-16 items-center border-b border-slate-200 px-6">
            <div className="flex items-center gap-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-md bg-blue-600 text-sm font-semibold text-white">
                S
              </div>
              <div>
                <div className="text-sm font-semibold text-slate-900">SMAP</div>
                <div className="text-xs text-slate-500">Software Metrics</div>
              </div>
            </div>
          </div>

          <nav className="p-4">
            <div className="space-y-1">
              {sidebarItems.map(item => (
                <button
                  key={item.key}
                  type="button"
                  className={[
                    'flex h-10 w-full items-center justify-between rounded-lg px-3 text-left text-sm font-medium transition',
                    item.active
                      ? 'bg-blue-50 text-blue-700'
                      : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                  ].join(' ')}
                >
                  <span className="flex items-center gap-3">
                    <SidebarIcon />
                    {item.label}
                  </span>
                  {item.badge ? (
                    <span className="rounded bg-slate-200 px-2 py-0.5 text-xs text-slate-600">
                      {item.badge}
                    </span>
                  ) : null}
                </button>
              ))}
            </div>
          </nav>
        </aside>

        <div className="min-w-0">
          <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-8">
            <div>
              <div className="text-xs font-medium uppercase tracking-[0.08em] text-slate-400">
                项目管理页
              </div>
              <div className="text-lg font-semibold text-slate-900">项目状态 Dashboard</div>
            </div>
            <div className="flex items-center gap-3">
              <button
                type="button"
                className="inline-flex h-9 items-center rounded-lg border border-slate-300 px-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                帮助
              </button>
              <button
                type="button"
                className="inline-flex h-9 items-center rounded-lg border border-slate-300 px-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                王晨
              </button>
            </div>
          </header>

          <main className="p-8">
            <div className="mx-auto max-w-[1600px] space-y-6">
              <section className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-2">
                  <div className="flex items-center gap-3">
                    <h1 className="text-[28px] font-semibold leading-9 text-slate-950">
                      {projectSummary.name}
                    </h1>
                    <StatusBadge tone={projectSummary.statusTone}>
                      {projectSummary.status}
                    </StatusBadge>
                  </div>
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-slate-500">
                    <span>负责人：{projectSummary.owner}</span>
                    <span>分支：{projectSummary.branch}</span>
                    <span>最近更新：{projectSummary.updatedAt}</span>
                  </div>
                </div>

                <div className="flex flex-wrap gap-3">
                  <button
                    type="button"
                    className="inline-flex h-10 items-center rounded-lg border border-slate-300 bg-white px-4 text-sm font-medium text-slate-700 hover:bg-slate-50"
                  >
                    切换项目
                  </button>
                  <button
                    type="button"
                    className="inline-flex h-10 items-center rounded-lg border border-slate-300 bg-white px-4 text-sm font-medium text-slate-700 hover:bg-slate-50"
                  >
                    导出报告
                  </button>
                  <button
                    type="button"
                    className="inline-flex h-10 items-center rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
                  >
                    上传代码
                  </button>
                </div>
              </section>

              <section className="grid gap-4 xl:grid-cols-4 md:grid-cols-2">
                {kpiCards.map(card => (
                  <article
                    key={card.label}
                    className="flex h-28 flex-col justify-between rounded-xl border border-slate-200 bg-white p-5"
                  >
                    <div className="text-sm font-medium text-slate-500">{card.label}</div>
                    <div className="flex items-end justify-between gap-3">
                      <div className="text-[28px] font-semibold leading-none text-slate-950">
                        {card.value}
                      </div>
                      <StatusBadge tone={card.tone}>{card.delta}</StatusBadge>
                    </div>
                  </article>
                ))}
              </section>

              <section className="grid gap-6 xl:grid-cols-[minmax(0,1.7fr)_minmax(340px,1fr)]">
                <article className="overflow-hidden rounded-xl border border-slate-200 bg-white">
                  <SectionTitle title="项目概览" description="当前仓库与质量状态" />
                  <div className="grid gap-4 p-6 md:grid-cols-2">
                    <div className="space-y-4">
                      <div className="rounded-lg bg-slate-50 p-4">
                        <div className="text-xs font-medium uppercase tracking-[0.08em] text-slate-400">
                          Repository
                        </div>
                        <div className="mt-2 break-all text-sm text-slate-700">
                          {projectSummary.repository}
                        </div>
                      </div>
                      <div className="grid gap-4 sm:grid-cols-3">
                        {taskSummaries.map(item => (
                          <div key={item.label} className="rounded-lg border border-slate-200 p-4">
                            <div className="text-xs font-medium uppercase tracking-[0.08em] text-slate-400">
                              {item.label}
                            </div>
                            <div className="mt-3 text-xl font-semibold text-slate-950">
                              {item.value}
                            </div>
                            <div className="mt-1 text-xs text-slate-500">{item.helper}</div>
                          </div>
                        ))}
                      </div>
                    </div>

                    <div className="rounded-lg border border-slate-200 p-4">
                      <div className="mb-4 flex items-center justify-between">
                        <div className="text-sm font-medium text-slate-700">项目状态</div>
                        <StatusBadge tone="success">质量稳定</StatusBadge>
                      </div>
                      <div className="space-y-4">
                        <div>
                          <div className="mb-2 flex items-center justify-between text-sm text-slate-600">
                            <span>代码健康度</span>
                            <span>82%</span>
                          </div>
                          <div className="h-2 rounded-full bg-slate-100">
                            <div className="h-2 w-[82%] rounded-full bg-blue-600" />
                          </div>
                        </div>
                        <div>
                          <div className="mb-2 flex items-center justify-between text-sm text-slate-600">
                            <span>测试覆盖率</span>
                            <span>74%</span>
                          </div>
                          <div className="h-2 rounded-full bg-slate-100">
                            <div className="h-2 w-[74%] rounded-full bg-slate-700" />
                          </div>
                        </div>
                        <div>
                          <div className="mb-2 flex items-center justify-between text-sm text-slate-600">
                            <span>任务完成率</span>
                            <span>68%</span>
                          </div>
                          <div className="h-2 rounded-full bg-slate-100">
                            <div className="h-2 w-[68%] rounded-full bg-emerald-600" />
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </article>

                <article className="overflow-hidden rounded-xl border border-slate-200 bg-white">
                  <SectionTitle
                    title="当前任务状态"
                    description="本次分析任务与负责人"
                    action={
                      <button
                        type="button"
                        className="inline-flex h-8 items-center rounded-md border border-slate-300 px-3 text-sm text-slate-700 hover:bg-slate-50"
                      >
                        刷新
                      </button>
                    }
                  />
                  <div className="space-y-3 p-6">
                    {taskRows.slice(0, 3).map(task => (
                      <div
                        key={task.name}
                        className="rounded-lg border border-slate-200 px-4 py-3"
                      >
                        <div className="flex items-center justify-between gap-3">
                          <div>
                            <div className="text-sm font-medium text-slate-900">{task.name}</div>
                            <div className="mt-1 text-xs text-slate-500">
                              {task.owner} · {task.startedAt}
                            </div>
                          </div>
                          <StatusBadge tone={task.tone}>{task.status}</StatusBadge>
                        </div>
                      </div>
                    ))}
                  </div>
                </article>
              </section>

              <section className="overflow-hidden rounded-xl border border-slate-200 bg-white">
                <SectionTitle
                  title="文件管理"
                  description="拖拽上传、文件状态与处理进度"
                  action={
                    <div className="flex gap-2">
                      <button
                        type="button"
                        className="inline-flex h-8 items-center rounded-md border border-slate-300 px-3 text-sm text-slate-700 hover:bg-slate-50"
                      >
                        批量删除
                      </button>
                      <button
                        type="button"
                        className="inline-flex h-8 items-center rounded-md bg-blue-600 px-3 text-sm font-medium text-white hover:bg-blue-700"
                      >
                        新建上传
                      </button>
                    </div>
                  }
                />

                <div className="grid gap-6 p-6 xl:grid-cols-[360px_minmax(0,1fr)]">
                  <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50 p-6">
                    <div className="flex h-full min-h-[220px] flex-col items-center justify-center text-center">
                      <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-blue-50 text-blue-600">
                        <svg viewBox="0 0 24 24" className="h-6 w-6 fill-none stroke-current">
                          <path
                            d="M12 16V4m0 0-4 4m4-4 4 4M5 16v2a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-2"
                            strokeWidth="1.8"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          />
                        </svg>
                      </div>
                      <div className="text-base font-medium text-slate-900">拖拽文件到此处上传</div>
                      <div className="mt-2 text-sm text-slate-500">
                        支持 ZIP、JAVA、XMI、OOM
                      </div>
                      <button
                        type="button"
                        className="mt-6 inline-flex h-10 items-center rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
                      >
                        选择文件
                      </button>
                    </div>
                  </div>

                  <div className="overflow-hidden rounded-xl border border-slate-200">
                    <div className="grid grid-cols-[minmax(0,2fr)_96px_140px_96px_112px_120px] border-b border-slate-200 bg-slate-50 px-4 py-3 text-xs font-semibold uppercase tracking-[0.06em] text-slate-500">
                      <div>文件名</div>
                      <div>类型</div>
                      <div>更新时间</div>
                      <div>大小</div>
                      <div>状态</div>
                      <div>进度</div>
                    </div>
                    <div className="divide-y divide-slate-200">
                      {uploadItems.map(item => (
                        <div
                          key={item.name}
                          className="grid grid-cols-[minmax(0,2fr)_96px_140px_96px_112px_120px] items-center px-4 py-4 text-sm"
                        >
                          <div className="min-w-0">
                            <div className="truncate font-medium text-slate-900">{item.name}</div>
                            <div className="mt-1 text-xs text-slate-500">最近操作：质量扫描</div>
                          </div>
                          <div className="text-slate-600">{item.type}</div>
                          <div className="text-slate-600">{item.updatedAt}</div>
                          <div className="text-slate-600">{item.size}</div>
                          <div>
                            <StatusBadge tone={item.tone}>{item.status}</StatusBadge>
                          </div>
                          <div>
                            <div className="h-2 rounded-full bg-slate-100">
                              <div
                                className={[
                                  'h-2 rounded-full',
                                  item.tone === 'error'
                                    ? 'bg-rose-500'
                                    : item.tone === 'warning'
                                    ? 'bg-amber-500'
                                    : 'bg-blue-600'
                                ].join(' ')}
                                style={{ width: `${item.progress}%` }}
                              />
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </section>

              <section className="overflow-hidden rounded-xl border border-slate-200 bg-white">
                <SectionTitle title="历史任务记录" description="最近执行的分析任务" />
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-slate-200 text-sm">
                    <thead className="bg-slate-50">
                      <tr className="text-left text-xs font-semibold uppercase tracking-[0.06em] text-slate-500">
                        <th className="px-6 py-3">任务名称</th>
                        <th className="px-6 py-3">负责人</th>
                        <th className="px-6 py-3">状态</th>
                        <th className="px-6 py-3">开始时间</th>
                        <th className="px-6 py-3">耗时</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200 bg-white">
                      {taskRows.map(row => (
                        <tr key={`${row.name}-${row.startedAt}`} className="hover:bg-slate-50">
                          <td className="px-6 py-4 font-medium text-slate-900">{row.name}</td>
                          <td className="px-6 py-4 text-slate-600">{row.owner}</td>
                          <td className="px-6 py-4">
                            <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                          </td>
                          <td className="px-6 py-4 text-slate-600">{row.startedAt}</td>
                          <td className="px-6 py-4 text-slate-600">{row.duration}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>

              <section className="overflow-hidden rounded-xl border border-slate-200 bg-white">
                <SectionTitle
                  title="阈值配置"
                  description="统一管理质量预警阈值"
                  action={
                    <button
                      type="button"
                      className="inline-flex h-9 items-center rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
                    >
                      保存配置
                    </button>
                  }
                />
                <div className="grid gap-4 p-6 md:grid-cols-2 xl:grid-cols-3">
                  {thresholdFields.map(field => (
                    <div key={field.key} className="space-y-3 rounded-xl border border-slate-200 p-4">
                      <div className="text-sm font-medium text-slate-900">{field.label}</div>
                      <div className="grid gap-3 sm:grid-cols-3">
                        <label className="space-y-2">
                          <span className="text-xs font-medium text-slate-500">低风险</span>
                          <input
                            defaultValue={field.low}
                            className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm outline-none ring-0 transition focus:border-blue-500"
                          />
                        </label>
                        <label className="space-y-2">
                          <span className="text-xs font-medium text-slate-500">中风险</span>
                          <input
                            defaultValue={field.medium}
                            className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm outline-none ring-0 transition focus:border-blue-500"
                          />
                        </label>
                        <label className="space-y-2">
                          <span className="text-xs font-medium text-slate-500">高风险</span>
                          <input
                            defaultValue={field.high}
                            className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm outline-none ring-0 transition focus:border-blue-500"
                          />
                        </label>
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            </div>
          </main>
        </div>
      </div>
    </div>
  )
}
