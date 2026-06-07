'use client';

import ChatWindow from '@/components/chat/ChatWindow';

export default function ChatPage() {
  return (
    <div className="p-8 max-w-6xl mx-auto animate-in fade-in duration-500">
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight">AI 智能对话</h1>
        <p className="text-muted-foreground text-sm">
          支持多模型动态切换、流式秒级响应、具备长短期记忆能力
        </p>
      </div>
      
      <ChatWindow />
    </div>
  );
}
