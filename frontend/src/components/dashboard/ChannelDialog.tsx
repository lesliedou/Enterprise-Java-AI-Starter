'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { 
  Button, 
  Input, 
  Label,
  Card,
  CardContent,
  CardHeader,
  CardTitle
} from '@/components/ui';
import { LlmChannelConfig, LlmProvider } from '@/types';

const channelSchema = z.object({
  name: z.string().min(2, '名称至少 2 个字符'),
  provider: z.enum(['OPENAI', 'DEEPSEEK', 'CLAUDE', 'QWEN', 'ZHIPU']),
  apiKey: z.string().min(1, 'API Key 不能为空'),
  baseUrl: z.string().url('请输入有效的 URL'),
  modelName: z.string().min(1, '模型名称不能为空'),
  weight: z.number().min(0).max(100),
  priority: z.number().min(1),
  isEnabled: z.boolean().default(true),
  timeoutMillis: z.number().min(1000).default(30000),
});

type ChannelFormValues = z.infer<typeof channelSchema>;

interface ChannelDialogProps {
  initialData?: LlmChannelConfig;
  onSave: (data: ChannelFormValues) => void;
  onClose: () => void;
}

export function ChannelDialog({ initialData, onSave, onClose }: ChannelDialogProps) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ChannelFormValues>({
    resolver: zodResolver(channelSchema),
    defaultValues: initialData || {
      name: '',
      provider: 'OPENAI',
      apiKey: '',
      baseUrl: 'https://api.openai.com/v1',
      modelName: 'gpt-4',
      weight: 100,
      priority: 1,
      isEnabled: true,
      timeoutMillis: 30000,
    },
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm animate-in fade-in duration-300">
      <Card className="w-full max-w-lg shadow-2xl">
        <CardHeader>
          <CardTitle>{initialData ? '编辑渠道' : '新建 AI 渠道'}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSave)} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="name">渠道名称</Label>
                <Input id="name" {...register('name')} placeholder="如：生产环境 OpenAI" />
                {errors.name && <p className="text-xs text-red-500">{errors.name.message}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="provider">供应商</Label>
                <select 
                  id="provider" 
                  {...register('provider')}
                  className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                >
                  <option value="OPENAI">OpenAI</option>
                  <option value="DEEPSEEK">DeepSeek</option>
                  <option value="CLAUDE">Claude</option>
                  <option value="QWEN">通义千问</option>
                  <option value="ZHIPU">智谱 AI</option>
                </select>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="apiKey">API Key</Label>
              <Input id="apiKey" type="password" {...register('apiKey')} placeholder="sk-..." />
              {errors.apiKey && <p className="text-xs text-red-500">{errors.apiKey.message}</p>}
            </div>

            <div className="space-y-2">
              <Label htmlFor="baseUrl">Base URL</Label>
              <Input id="baseUrl" {...register('baseUrl')} placeholder="https://..." />
              {errors.baseUrl && <p className="text-xs text-red-500">{errors.baseUrl.message}</p>}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="modelName">模型名称</Label>
                <Input id="modelName" {...register('modelName')} placeholder="gpt-4-turbo" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="priority">优先级 (P1最高)</Label>
                <Input id="priority" type="number" {...register('priority', { valueAsNumber: true })} />
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex justify-between">
                <Label>权重分发 (0-100)</Label>
                <span className="text-xs font-mono text-blue-600">当前: {100}%</span>
              </div>
              <Input type="range" min="0" max="100" {...register('weight', { valueAsNumber: true })} className="cursor-pointer" />
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t">
              <Button type="button" variant="outline" onClick={onClose}>取消</Button>
              <Button type="submit" className="bg-blue-600 hover:bg-blue-700" disabled={isSubmitting}>
                {isSubmitting ? '保存中...' : '确认保存'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
