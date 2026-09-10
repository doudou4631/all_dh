function baseMeta(config = {}) {
  return config.mode === "live" ? "live" : "mock"
}

function toStatusLabel(status) {
  switch (status) {
    case "completed":
      return "Completed"
    case "active":
      return "Running"
    case "ready":
      return "Ready"
    case "blocked":
      return "Blocked"
    case "skipped":
      return "Skipped"
    default:
      return "Pending"
  }
}

function withStatus(step, status, note = "") {
  return {
    ...step,
    status,
    note,
    statusLabel: toStatusLabel(status),
  }
}

function getBaseSteps(task, config = {}) {
  const modeText = baseMeta(config)
  const orderLabel = task?.orderId || "pending order session"
  const phoneLabel = task?.phone || "unknown phone"

  return [
    {
      key: "answers-start",
      title: "Create questionnaire session",
      description: `Create a Teddy questionnaire session for ${phoneLabel} and bind it to ${orderLabel}.`,
      endpoint: config.answersStartUrl || "/api/v1/answers/start",
      meta: `${modeText} / answers-start`,
    },
    {
      key: "check-applet-phone",
      title: "Verify phone binding",
      description: "Check whether the current wxCode/openId can continue the appeal flow.",
      endpoint: config.checkAppletPhoneUrl || "/api/v1/forward/inter/checkAppletPhone",
      meta: `${modeText} / check-applet-phone`,
    },
    {
      key: "submit-answers",
      title: "Submit questionnaire answers",
      description: "Replay the configured answers in sequence and advance the questionnaire.",
      endpoint: config.answersSubmitUrl || "/api/v1/answers/submit-and-next",
      meta: `${modeText} / submit-answers`,
    },
    {
      key: "upload-evidence",
      title: "Upload evidence image",
      description: "Upload the placeholder evidence image for the attachment step.",
      endpoint: config.answersSubmitUrl || "/api/v1/answers/submit-and-next",
      meta: `${modeText} / upload-evidence`,
    },
    {
      key: "query-progress",
      title: "Query processing progress",
      description: "Poll the upstream progress endpoint and write back the latest result.",
      endpoint: config.queryProgressUrl || "/api/v1/forward/inter/queryAppletProgressList",
      meta: `${modeText} / query-progress`,
    },
  ]
}

function inferPlanFromTask(task, config = {}) {
  const steps = getBaseSteps(task, config)
  if (task?.status === "completed") {
    return steps.map((step) => withStatus(step, "completed", "This step has already been completed."))
  }

  if (task?.status === "failed") {
    return steps.map((step, index) =>
      withStatus(
        step,
        index === 0 ? "blocked" : "pending",
        index === 0 ? "The task is currently blocked and requires manual review." : "Waiting for the blocked step to be resolved.",
      ),
    )
  }

  if (task?.status === "processing") {
    return steps.map((step, index) => {
      if (index < 2) return withStatus(step, "completed", "The prerequisite session steps are complete.")
      if (index === 2) return withStatus(step, "active", "The questionnaire submission is currently running.")
      return withStatus(step, "pending", "Waiting for the previous step to finish.")
    })
  }

  return steps.map((step, index) =>
    withStatus(
      step,
      index === 0 ? "ready" : "pending",
      index === 0 ? "The task is ready to start the upstream workflow." : "Waiting for the previous step to finish.",
    ),
  )
}

export function buildTdxExecutionPlan(task, config = {}, options = {}) {
  const explicitSteps = options.execution?.result?.steps || options.execution?.steps
  if (Array.isArray(explicitSteps) && explicitSteps.length) {
    return explicitSteps.map((step) => ({
      ...step,
      statusLabel: step.statusLabel || toStatusLabel(step.status),
    }))
  }

  const phase = options.phase || "task"
  const steps = getBaseSteps(task, config)

  if (phase === "completed") {
    return steps.map((step) => withStatus(step, "completed", "Marked as completed by the current executor."))
  }

  if (phase === "planned-live") {
    return steps.map((step, index) =>
      withStatus(
        step,
        index === 0 ? "ready" : "pending",
        index === 0 ? "The live execution chain is ready to start here." : "Waiting for the previous step to succeed.",
      ),
    )
  }

  return inferPlanFromTask(task, config)
}

export function summarizeTdxExecutionPlan(plan = []) {
  const totalSteps = plan.length
  const completedSteps = plan.filter((step) => step.status === "completed").length
  const activeSteps = plan.filter((step) => step.status === "active").length
  const blockedSteps = plan.filter((step) => step.status === "blocked").length
  const readySteps = plan.filter((step) => step.status === "ready").length
  const skippedSteps = plan.filter((step) => step.status === "skipped").length
  const pendingSteps = plan.filter((step) => step.status === "pending").length
  const nextStep =
    plan.find((step) => ["active", "ready", "blocked", "pending"].includes(step.status)) ||
    plan[plan.length - 1] ||
    null

  let overallStatus = "pending"
  if (blockedSteps) {
    overallStatus = "blocked"
  } else if (completedSteps + skippedSteps === totalSteps && totalSteps > 0) {
    overallStatus = "completed"
  } else if (activeSteps || readySteps) {
    overallStatus = "in-progress"
  }

  return {
    totalSteps,
    completedSteps,
    activeSteps,
    blockedSteps,
    readySteps,
    skippedSteps,
    pendingSteps,
    nextStepTitle: nextStep?.title || "",
    overallStatus,
  }
}
