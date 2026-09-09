function buildReadinessChecks(config = {}, isLive = false) {
  const checks = [
    {
      key: 'qmf-credentials',
      title: 'QMF 凭据',
      status: config.qmfUsername && config.qmfPassword && config.qmfAppId ? 'ready' : 'blocked',
      description: '需要配置 QMF 用户名、密码和 AppId，才能稳定获取 orderId / wxCode。',
    },
    {
      key: 'sms-chain',
      title: '短信发送链路',
      status: config.sendSmsUrl && config.loginSmsUrl ? 'ready' : 'blocked',
      description: '需要配置 sendSms 与 login-sms 接口，才能完成验证码发送和短信登录。',
    },
    {
      key: 'qmf-code-chain',
      title: 'QMF 取码链路',
      status: config.qmfLoginUrl && config.qmfCodeUrl ? 'ready' : 'blocked',
      description: '需要配置 QMF 登录接口和取码接口，才能获取 orderId / wxCode。',
    },
    {
      key: 'execution-endpoints',
      title: '问卷执行链路',
      status:
        config.answersStartUrl &&
        config.checkAppletPhoneUrl &&
        config.answersSubmitUrl &&
        config.queryProgressUrl
          ? isLive
            ? 'planned'
            : 'ready'
          : 'blocked',
      description: '需要配置问卷开始、手机号校验、问卷提交和进度查询接口，才能继续打通 live 清理执行。',
    },
  ]

  return checks
}

export function buildTdxExecutorProfile(config = {}) {
  const isLive = config.mode === 'live'
  const hasBaseConfig = Boolean(config.originalIp && config.qmfUsername && config.qmfAppId)
  const hasCredentials = Boolean(config.qmfUsername && config.qmfPassword && config.qmfAppId)
  const hasExecutionEndpoints = Boolean(
    config.answersStartUrl &&
      config.checkAppletPhoneUrl &&
      config.answersSubmitUrl &&
      config.queryProgressUrl,
  )

  const capabilities = [
    {
      key: 'send-sms',
      title: '发送短信验证码',
      status: isLive ? (config.sendSmsUrl ? 'ready' : 'blocked') : 'ready',
      description: isLive
        ? '使用已配置的 sendSms 接口向目标手机号发送验证码。'
        : 'mock 执行器始终可以模拟短信下发。',
    },
    {
      key: 'allocate-order-session',
      title: '分配 orderId 会话',
      status: isLive ? (hasCredentials ? 'ready' : 'blocked') : 'ready',
      description: isLive
        ? '获取 QMF 的 orderId 和 wxCode，供后续清理流程复用。'
        : 'mock 执行器会生成可复用的内存会话。',
    },
    {
      key: 'login-sms',
      title: 'login-sms 登录会话',
      status: isLive ? (config.loginSmsUrl && hasBaseConfig ? 'ready' : 'blocked') : 'ready',
      description: isLive
        ? '通过 login-sms 完成短信登录，并建立泰迪熊清理会话。'
        : 'mock 执行器会模拟 login-sms 成功和 token 颁发。',
    },
    {
      key: 'execute-cleanup',
      title: '执行清理工作流',
      status: isLive ? (hasExecutionEndpoints ? 'planned' : 'blocked') : 'ready',
      description: isLive
        ? 'JS 已定义执行计划，但 live 版问卷提交、证据上传还在等待真实上游适配器接入。'
        : 'mock 执行器会跑完整个 JS 清理生命周期，并把全部步骤标记为完成。',
    },
    {
      key: 'progress-polling',
      title: '进度轮询',
      status: isLive ? (hasExecutionEndpoints ? 'planned' : 'blocked') : 'ready',
      description: isLive
        ? '进度查询步骤已经在 JS 中建模，但 live 轮询仍需要真实上游执行器。'
        : 'mock 执行器会直接返回完成态结果。',
    },
  ]

  const readinessChecks = buildReadinessChecks(config, isLive)

  const blockingItems = [
    ...capabilities.filter((item) => item.status === 'blocked').map((item) => item.title),
    ...readinessChecks.filter((item) => item.status === 'blocked').map((item) => item.title),
  ].filter((item, index, array) => array.indexOf(item) === index)

  const ready = capabilities.every((item) => item.status === 'ready')
  const summary = {
    mode: config.mode || 'mock',
    executorName: isLive ? 'tdx-live-plan-executor' : 'tdx-mock-executor',
    ready,
    readyCount: capabilities.filter((item) => item.status === 'ready').length,
    plannedCount: capabilities.filter((item) => item.status === 'planned').length,
    blockedCount: capabilities.filter((item) => item.status === 'blocked').length,
  }

  return {
    ...summary,
    capabilities,
    readinessChecks,
    blockingItems,
    nextTarget: isLive
      ? '继续把问卷提交、证据上传、进度轮询接到真实 JS 上游执行器。'
      : '先补齐 live 配置，再切换到真实运营模式。',
  }
}
