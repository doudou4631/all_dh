import { readFile, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const storePath = resolve(process.cwd(), 'data', 'tdx-tasks.json')
const MAX_TASK_NOTES = 200

const fallbackTasks = []

async function readTasks() {
  try {
    const raw = await readFile(storePath, 'utf-8')
    return JSON.parse(raw)
  } catch {
    return [...fallbackTasks]
  }
}

async function writeTasks(tasks) {
  const normalizedTasks = tasks.map((task) => limitTaskNotes(task))
  await writeFile(storePath, JSON.stringify(normalizedTasks, null, 2), 'utf-8')
  return normalizedTasks
}

function limitTaskNotes(task = {}) {
  if (!Array.isArray(task.notes)) return task
  return { ...task, notes: task.notes.slice(0, MAX_TASK_NOTES) }
}

export async function listTdxTasks() {
  const tasks = await readTasks()
  return [...tasks].sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))
}

export async function createTdxTask(task) {
  const tasks = await readTasks()
  const nextTask = limitTaskNotes(task)
  tasks.unshift(nextTask)
  await writeTasks(tasks)
  return nextTask
}

export async function findTdxTask(id) {
  const tasks = await readTasks()
  return tasks.find((item) => item.id === id) || null
}

export async function updateTdxTask(id, patch) {
  const tasks = await readTasks()
  const index = tasks.findIndex((item) => item.id === id)
  if (index < 0) return null
  tasks[index] = limitTaskNotes({ ...tasks[index], ...patch })
  await writeTasks(tasks)
  return tasks[index]
}
