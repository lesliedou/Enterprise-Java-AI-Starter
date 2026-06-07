import { ApiResponse, LlmChannelConfig } from '@/types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

export const apiClient = {
  async get<T>(path: string): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      headers: {
        'Content-Type': 'application/json',
        'X-App-Key': 'admin-master-key', // 示例鉴权
      },
    });
    const result: ApiResponse<T> = await response.json();
    if (result.code !== 200) {
      throw new Error(result.message);
    }
    return result.data;
  },

  async post<T>(path: string, data: any): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-App-Key': 'admin-master-key',
      },
      body: JSON.stringify(data),
    });
    const result: ApiResponse<T> = await response.json();
    if (result.code !== 200) {
      throw new Error(result.message);
    }
    return result.data;
  },

  // 渠道管理相关
  channels: {
    list: () => apiClient.get<LlmChannelConfig[]>('/admin/channels'),
    save: (data: Partial<LlmChannelConfig>) => apiClient.post<LlmChannelConfig>('/admin/channels', data),
    delete: (id: string) => apiClient.post<void>(`/admin/channels/delete/${id}`, {}),
  },
};
