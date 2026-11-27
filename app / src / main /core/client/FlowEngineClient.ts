import type {
  FlowApiEnvelope,
  FlowExecuteRequest,
  FlowExecuteResponse,
  FlowStatusRequest,
  FlowStatusResponse,
  FlowCancelRequest,
  FlowCancelResponse,
  FlowDeleteRequest,
  FlowDeleteResponse,
  FlowTaskShape
} from '@shared/ipc/flowEngine.api.types'
import { FLOW_ENGINE_HTTP_TIMEOUT_MS } from '@shared/constant/flowEngine'

export class FlowEngineClient {
  private readonly baseUrl: string

  constructor(hostIp: string, port: number) {
    this.baseUrl = `http://${hostIp}:${port}`
  }

  async execute(request: FlowExecuteRequest): Promise<FlowExecuteResponse> {
    return this.post<FlowExecuteRequest, FlowExecuteResponse>('/flow/execute', request)
  }

  async status(request: FlowStatusRequest): Promise<FlowStatusResponse> {
    return this.post<FlowStatusRequest, FlowStatusResponse>('/flow/status', request)
  }

  async cancel(request: FlowCancelRequest): Promise<FlowCancelResponse> {
    return this.post<FlowCancelRequest, FlowCancelResponse>('/flow/cancel', request)
  }

  async delete(request: FlowDeleteRequest): Promise<FlowDeleteResponse> {
    return this.post<FlowDeleteRequest, FlowDeleteResponse>('/flow/delete', request)
  }

  async getTask(taskId: string): Promise<FlowTaskShape | null> {
    const url = `${this.baseUrl}/flow/task/${encodeURIComponent(taskId)}`
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), FLOW_ENGINE_HTTP_TIMEOUT_MS)

    try {
      const response = await fetch(url, { signal: controller.signal })
      if (response.status === 404) return null
      const envelope = (await response.json()) as FlowApiEnvelope<FlowTaskShape>
      if (envelope.code !== 200 || !envelope.data) return null
      return envelope.data
    } catch (err) {
      if (err instanceof Error && err.name === 'AbortError') {
        throw new Error(
          `flow-engine /flow/task/${taskId} timeout after ${FLOW_ENGINE_HTTP_TIMEOUT_MS}ms`
        )
      }
      throw err
    } finally {
      clearTimeout(timer)
    }
  }

  private async post<Req, Res>(path: string, body: Req): Promise<Res> {
    const url = `${this.baseUrl}${path}`
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), FLOW_ENGINE_HTTP_TIMEOUT_MS)

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
        signal: controller.signal
      })

      const envelope = (await response.json()) as FlowApiEnvelope<Res>
      if (envelope.code !== 200 || !envelope.data) {
        throw new Error(`flow-engine ${path} failed: code=${envelope.code} ${envelope.message}`)
      }
      return envelope.data
    } catch (err) {
      if (err instanceof Error && err.name === 'AbortError') {
        throw new Error(`flow-engine ${path} timeout after ${FLOW_ENGINE_HTTP_TIMEOUT_MS}ms`)
      }
      throw err
    } finally {
      clearTimeout(timer)
    }
  }
}
