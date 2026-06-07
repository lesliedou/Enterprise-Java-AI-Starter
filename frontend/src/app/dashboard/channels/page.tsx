'use client';

import { useState } from 'react';
import { useModelStore } from '@/store/useModelStore';
import { 
  Table, 
  TableBody, 
  TableCell, 
  TableHead, 
  TableHeader, 
  TableRow,
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Input,
  Label
} from '@/components/ui';
import { Settings2, Plus, Search, Trash2, ShieldAlert } from 'lucide-react';
import { LlmChannelConfig, LlmProvider } from '@/types';
import { ChannelDialog } from '@/components/dashboard/ChannelDialog';

export default function ChannelsPage() {
  const { channels, updateChannel } = useModelStore();
  const [searchTerm, setSearchTerm] = useState('');
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingChannel, setEditingChannel] = useState<LlmChannelConfig | undefined>();

  const getStatusBadge = (status?: string) => {
    switch (status) {
      case 'ACTIVE': return <Badge variant="success">运行中</Badge>;
      case 'CIRCUIT_OPEN': return <Badge variant="destructive">已熔断</Badge>;
      case 'DISABLED': return <Badge variant="outline">已禁用</Badge>;
      default: return <Badge variant="outline">未知</Badge>;
    }
  };

  const filteredChannels = channels.length > 0 ? channels.filter(c => 
    c.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    c.provider.toLowerCase().includes(searchTerm.toLowerCase())
  ) : [
    { id: 'ch-001', name: '主用 OpenAI', provider: 'OPENAI' as LlmProvider, modelName: 'gpt-4-turbo', weight: 80, priority: 1, isEnabled: true, status: 'ACTIVE', apiKey: '***', baseUrl: 'https://api.openai.com/v1', timeoutMillis: 30000 },
    { id: 'ch-002', name: '备用 DeepSeek', provider: 'DEEPSEEK' as LlmProvider, modelName: 'deepseek-chat', weight: 20, priority: 1, isEnabled: true, status: 'ACTIVE', apiKey: '***', baseUrl: 'https://api.deepseek.com', timeoutMillis: 30000 },
    { id: 'ch-003', name: '三方 Qwen', provider: 'QWEN' as LlmProvider, modelName: 'qwen-max', weight: 100, priority: 2, isEnabled: false, status: 'DISABLED', apiKey: '***', baseUrl: 'https://dashscope.aliyuncs.com', timeoutMillis: 30000 },
    { id: 'ch-004', name: '测试 Zhipu', provider: 'ZHIPU' as LlmProvider, modelName: 'glm-4', weight: 0, priority: 3, isEnabled: true, status: 'CIRCUIT_OPEN', apiKey: '***', baseUrl: 'https://open.bigmodel.cn/api/paas/v4', timeoutMillis: 30000 },
  ];

  const handleEdit = (channel: any) => {
    setEditingChannel(channel);
    setIsDialogOpen(true);
  };

  const handleSave = (data: any) => {
    console.log('保存渠道数据:', data);
    // 实际应调用 API
    setIsDialogOpen(false);
    setEditingChannel(undefined);
  };

  return (
    <div className="p-8 space-y-6 animate-in fade-in duration-500 relative">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">渠道管理</h1>
          <p className="text-muted-foreground">管理并配置不同的大模型供应商接入渠道</p>
        </div>
        <Button 
          className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700"
          onClick={() => {
            setEditingChannel(undefined);
            setIsDialogOpen(true);
          }}
        >
          <Plus className="h-4 w-4" /> 新建渠道
        </Button>
      </div>

      {isDialogOpen && (
        <ChannelDialog 
          initialData={editingChannel}
          onSave={handleSave}
          onClose={() => setIsDialogOpen(false)}
        />
      )}

      <Card className="border-none shadow-sm">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="text-lg">渠道列表</CardTitle>
            <div className="relative w-64">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="搜索渠道或模型..."
                className="pl-8 h-9 border-slate-200"
                value={searchTerm}
                onChange={(e: any) => setSearchTerm(e.target.value)}
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader className="bg-slate-50/50">
              <TableRow>
                <TableHead className="w-[200px]">渠道名称</TableHead>
                <TableHead>模型商 / 模型</TableHead>
                <TableHead>状态</TableHead>
                <TableHead>权重 (1-100)</TableHead>
                <TableHead>优先级</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredChannels.map((channel) => (
                <TableRow key={channel.id} className="hover:bg-slate-50/50 transition-colors">
                  <TableCell className="font-medium">{channel.name}</TableCell>
                  <TableCell>
                    <div className="flex flex-col">
                       <span className="text-sm">{channel.provider}</span>
                       <span className="text-xs text-muted-foreground font-mono">{channel.modelName}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    {getStatusBadge(channel.status as string)}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2">
                       <div className="w-12 h-1.5 bg-slate-100 rounded-full overflow-hidden">
                          <div className="bg-blue-500 h-full" style={{ width: `${channel.weight}%` }} />
                       </div>
                       <span className="text-xs">{channel.weight}%</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline" className="font-mono">P{channel.priority}</Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Button 
                        variant="outline" 
                        className="h-8 px-2 text-xs border-slate-200 hover:bg-slate-100"
                        onClick={() => handleEdit(channel)}
                      >
                        <Settings2 className="h-3.5 w-3.5 mr-1" /> 配置
                      </Button>
                      <Button variant="outline" className="h-8 px-2 text-xs border-red-100 text-red-600 hover:bg-red-50 hover:border-red-200">
                        <Trash2 className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* 熔断状态提示 */}
      <div className="bg-amber-50 border border-amber-100 rounded-lg p-4 flex items-start gap-3">
         <ShieldAlert className="h-5 w-5 text-amber-600 mt-0.5" />
         <div className="text-sm text-amber-800">
            <p className="font-semibold">关于自动熔断机制</p>
            <p className="opacity-90">当渠道调用失败率超过 50% 或平均响应时间大于 3000ms 时，系统将自动进入熔断状态。熔断期间流量将自动路由至次高优先级渠道。</p>
         </div>
      </div>
    </div>
  );
}
