import { randomUUID } from 'node:crypto'
import { readFile, rename, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const storePath = resolve(process.cwd(), 'data', 'tdx-tasks.json')
const MAX_TASK_NOTES = 100

const fallbackTasks = []
let taskStoreQueue = Promise.resolve()

function withTaskStoreLock(work) {
  const previous = taskStoreQueue
  let release
  taskStoreQueue = new Promise((resolveQueue) => {
    release = resolveQueue
  })
  return previous.then(async () => {
    try {
      return await work()
    } finally {
      release()
    }
  })
}

async function readTasks() {
  try {
    const raw = await readFile(storePath, 'utf-8')
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      throw new Error('tdx-tasks.json must contain an array')
    }
    return parsed
  } catch (error) {
    if (error?.code === 'ENOENT') return [...fallbackTasks]
    throw new Error(`Failed to read tdx task store: ${error instanceof Error ? error.message : String(error)}`)
  }
}

async function writeTasks(tasks) {
  const tempPath = `${storePath}.${process.pid}.${Date.now()}.${randomUUID()}.tmp`
  const compactTasks = tasks.map((task) => compactTdxTaskForStorage(task))
  try {
    await writeFile(tempPath, JSON.stringify(compactTasks, null, 2), 'utf-8')
    await rename(tempPath, storePath)
  } catch (error) {
    try {
      await rename(tempPath, `${tempPath}.failed`)
    } catch {
      // Preserve the original write error without masking it with cleanup failure.
    }
    throw new Error(`Failed to write tdx task store: ${error instanceof Error ? error.message : String(error)}`)
  }
  return compactTasks
}

export function compactTdxTaskForStorage(task = {}) {
  if (!task || typeof task !== 'object' || !Array.isArray(task.notes) || task.notes.length <= MAX_TASK_NOTES) {
    return task
  }
  return {
    ...task,
    notes: task.notes.slice(0, MAX_TASK_NOTES),
  }
}

export async function listTdxTasks() {
  return withTaskStoreLock(async () => {
    const tasks = await readTasks()
    return [...tasks].sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))
  })
}

export async function createTdxTask(task) {
  return withTaskStoreLock(async () => {
    const tasks = await readTasks()
    tasks.unshift(task)
    await writeTasks(tasks)
    return task
  })
}

export async function findTdxTask(id) {
  return withTaskStoreLock(async () => {
    const tasks = await readTasks()
    return tasks.find((item) => item.id === id) || null
  })
}

export async function updateTdxTask(id, patch) {
  return withTaskStoreLock(async () => {
    const tasks = await readTasks()
    const index = tasks.findIndex((item) => item.id === id)
    if (index < 0) return null
    tasks[index] = { ...tasks[index], ...patch }
    await writeTasks(tasks)
    return tasks[index]
  })
}
