export type LlmProvider = 'OPENAI' | 'DEEPSEEK' | 'CLAUDE' | 'QWEN' | 'ZHIPU';

export interface LlmChannelConfig {
  id: string;
  name: string;
  provider: LlmProvider;
  apiKey: string;
  baseUrl: string;
  modelName: string;
  weight: number;
  priority: number;
  isEnabled: boolean;
  timeoutMillis: number;
  status?: 'ACTIVE' | 'DISABLED' | 'CIRCUIT_OPEN'; // 扩展状态
}

export interface DashboardStats {
  todayTokenUsage: number;
  realtimeQps: number;
  cacheHitRate: number; // 语义缓存命中率
  channelHealth: {
    online: number;
    total: number;
  };
  channelUsage: {
    name: string;
    usage: number; // Token 消耗占比
    color: string;
  }[];
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}
