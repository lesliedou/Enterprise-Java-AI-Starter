import { create } from 'zustand';
import { LlmChannelConfig, DashboardStats } from '../types';

interface ModelState {
  channels: LlmChannelConfig[];
  stats: DashboardStats;
  loading: boolean;
  setChannels: (channels: LlmChannelConfig[]) => void;
  setStats: (stats: DashboardStats) => void;
  setLoading: (loading: boolean) => void;
  updateChannel: (id: string, updates: Partial<LlmChannelConfig>) => void;
}

export const useModelStore = create<ModelState>((set) => ({
  channels: [],
  loading: false,
  stats: {
    todayTokenUsage: 856200,
    realtimeQps: 4.2,
    cacheHitRate: 24.5,
    channelHealth: { online: 3, total: 4 },
    channelUsage: [
      { name: 'OpenAI GPT-4', usage: 65, color: 'bg-blue-500' },
      { name: 'DeepSeek Chat', usage: 20, color: 'bg-emerald-500' },
      { name: 'Qwen Max', usage: 10, color: 'bg-orange-500' },
      { name: 'Zhipu GLM-4', usage: 5, color: 'bg-purple-500' },
    ],
  },
  setChannels: (channels: LlmChannelConfig[]) => set({ channels }),
  setStats: (stats: DashboardStats) => set({ stats }),
  setLoading: (loading: boolean) => set({ loading }),
  updateChannel: (id: string, updates: Partial<LlmChannelConfig>) =>
    set((state: ModelState) => ({
      channels: state.channels.map((c: LlmChannelConfig) => (c.id === id ? { ...c, ...updates } : c)),
    })),
}));
