'use client';

import { useModelStore } from '@/store/useModelStore';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui';
import { 
  Activity, 
  Cpu, 
  ShieldCheck, 
  Database,
  ArrowUpRight,
  ChevronRight
} from 'lucide-react';

export default function DashboardPage() {
  const { stats } = useModelStore();

  const metrics = [
    {
      title: '今日 Token 总消耗',
      value: stats.todayTokenUsage.toLocaleString(),
      icon: Cpu,
      trend: '+12.5%',
      color: 'text-blue-600',
      bg: 'bg-blue-50',
    },
    {
      title: '实时请求 QPS',
      value: stats.realtimeQps.toFixed(1),
      icon: Activity,
      trend: '稳定',
      color: 'text-emerald-600',
      bg: 'bg-emerald-50',
    },
    {
      title: '语义缓存命中率',
      value: `${stats.cacheHitRate}%`,
      icon: Database,
      trend: '节省 2.4k$',
      color: 'text-orange-600',
      bg: 'bg-orange-50',
    },
    {
      title: '渠道健康度',
      value: `${stats.channelHealth.online}/${stats.channelHealth.total}`,
      icon: ShieldCheck,
      trend: '1 渠道熔断',
      color: 'text-purple-600',
      bg: 'bg-purple-50',
    },
  ];

  return (
    <div className="p-8 space-y-8 animate-in fade-in duration-500">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">监控大盘</h1>
          <p className="text-muted-foreground">实时掌握企业 AI 基础设施运行状态</p>
        </div>
        <div className="flex items-center gap-2 text-sm font-medium text-blue-600 cursor-pointer hover:underline">
          查看详细报表 <ChevronRight className="h-4 w-4" />
        </div>
      </div>

      {/* 核心指标卡片 */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {metrics.map((m) => (
          <Card key={m.title} className="border-none shadow-sm overflow-hidden group">
            <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
              <CardTitle className="text-sm font-medium">{m.title}</CardTitle>
              <div className={`${m.bg} p-2 rounded-lg group-hover:scale-110 transition-transform`}>
                <m.icon className={`h-4 w-4 ${m.color}`} />
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{m.value}</div>
              <div className="flex items-center text-xs mt-1 text-muted-foreground">
                <span className="text-emerald-500 font-medium flex items-center mr-1">
                   <ArrowUpRight className="h-3 w-3 mr-0.5" /> {m.trend}
                </span>
                较上周期
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* 渠道消耗分布 (纯 CSS 可视化) */}
      <Card className="border-none shadow-sm">
        <CardHeader>
          <CardTitle>各模型渠道 Token 消耗占比</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="w-full h-10 flex rounded-xl overflow-hidden border border-slate-100 shadow-inner">
            {stats.channelUsage.map((usage) => (
              <div 
                key={usage.name} 
                className={`${usage.color} h-full transition-all duration-1000 ease-out`}
                style={{ width: `${usage.usage}%` }}
                title={`${usage.name}: ${usage.usage}%`}
              />
            ))}
          </div>
          
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {stats.channelUsage.map((usage) => (
              <div key={usage.name} className="flex items-center gap-2">
                <div className={`w-3 h-3 rounded-full ${usage.color}`} />
                <div className="flex flex-col">
                   <span className="text-sm font-medium">{usage.name}</span>
                   <span className="text-xs text-muted-foreground">{usage.usage}%</span>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* 实时日志预览 (Mock) */}
      <div className="grid gap-4 md:grid-cols-2">
        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-base">最近 5 分钟请求趋势</CardTitle>
          </CardHeader>
          <CardContent className="h-48 flex items-end justify-between gap-1 pb-2">
             {[40, 70, 45, 90, 65, 30, 80, 55, 95, 40, 60, 85].map((h, i) => (
               <div 
                key={i} 
                className="bg-blue-100 w-full rounded-t-sm hover:bg-blue-500 transition-colors" 
                style={{ height: `${h}%` }} 
               />
             ))}
          </CardContent>
        </Card>
        
        <Card className="border-none shadow-sm">
           <CardHeader>
             <CardTitle className="text-base">高可用防护动态</CardTitle>
           </CardHeader>
           <CardContent className="space-y-4">
              <div className="flex items-start gap-3 text-sm">
                 <div className="w-2 h-2 rounded-full bg-orange-500 mt-1.5" />
                 <div className="flex-1">
                    <p className="font-medium">渠道 [OpenAI-Secondary] 已熔断</p>
                    <p className="text-xs text-muted-foreground">原因: 503 Service Unavailable · 2分钟前</p>
                 </div>
              </div>
              <div className="flex items-start gap-3 text-sm">
                 <div className="w-2 h-2 rounded-full bg-emerald-500 mt-1.5" />
                 <div className="flex-1">
                    <p className="font-medium">自动降级重试成功</p>
                    <p className="text-xs text-muted-foreground">已自动从 OpenAI 切换至 DeepSeek · 5分钟前</p>
                 </div>
              </div>
           </CardContent>
        </Card>
      </div>
    </div>
  );
}
