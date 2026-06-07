'use client';

import { useState, useRef, useEffect } from 'react';
import { Send, Bot, User, Loader2, RefreshCcw, AlertCircle } from 'lucide-react';
import { Markdown } from './Markdown';
import { Button, Input, Card } from '@/components/ui';
import { cn } from '@/lib/utils';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  status?: 'generating' | 'done' | 'error';
}

export default function ChatWindow() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  // 自动滚动到底部
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const handleSend = async () => {
    if (!input.trim() || isLoading) return;

    const userMessage: Message = { role: 'user', content: input };
    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    const assistantMessage: Message = { role: 'assistant', content: '', status: 'generating' };
    setMessages((prev) => [...prev, assistantMessage]);

    try {
      const response = await fetch('http://localhost:8080/api/v1/ai/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-App-Key': 'admin-master-key', // 商业化鉴权 Key
        },
        body: JSON.stringify({
          message: input,
          stream: true, // 开启 SSE 流式
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '服务异常');
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();
      let accumulatedContent = '';

      if (reader) {
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          const chunk = decoder.decode(value);
          const lines = chunk.split('\n');

          for (const line of lines) {
            if (line.startsWith('data:')) {
              try {
                const data = JSON.parse(line.slice(5));
                if (data.status === 'DONE') {
                  setMessages((prev) => {
                    const last = [...prev];
                    last[last.length - 1].status = 'done';
                    return last;
                  });
                  break;
                }
                
                if (data.status === 'ERROR') {
                    throw new Error(data.content || '生成中断');
                }

                accumulatedContent += data.content;
                setMessages((prev) => {
                  const last = [...prev];
                  last[last.length - 1].content = accumulatedContent;
                  return last;
                });
              } catch (e) {
                console.error('SSE 解析失败', e);
              }
            }
          }
        }
      }
    } catch (error: any) {
      console.error('对话失败', error);
      setMessages((prev) => {
        const last = [...prev];
        last[last.length - 1].status = 'error';
        last[last.length - 1].content = `[系统错误]: ${error.message}`;
        return last;
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card className="flex flex-col h-[calc(100vh-120px)] border-none shadow-xl bg-white dark:bg-slate-900">
      {/* 消息展示区 */}
      <div 
        ref={scrollRef}
        className="flex-1 overflow-y-auto p-6 space-y-6 scroll-smooth"
      >
        {messages.length === 0 && (
          <div className="h-full flex flex-col items-center justify-center text-muted-foreground opacity-50">
            <Bot className="h-12 w-12 mb-4" />
            <p>您好，我是企业级 AI 助手，请问有什么可以帮您？</p>
          </div>
        )}
        
        {messages.map((msg, i) => (
          <div 
            key={i} 
            className={cn(
              "flex items-start gap-4 animate-in slide-in-from-bottom-2 duration-300",
              msg.role === 'user' ? "flex-row-reverse" : "flex-row"
            )}
          >
            <div className={cn(
              "h-9 w-9 rounded-lg flex items-center justify-center shrink-0 shadow-sm",
              msg.role === 'user' ? "bg-blue-600 text-white" : "bg-slate-100 dark:bg-slate-800 text-blue-600"
            )}>
              {msg.role === 'user' ? <User size={20} /> : <Bot size={20} />}
            </div>
            
            <div className={cn(
              "max-w-[80%] rounded-2xl p-4 shadow-sm",
              msg.role === 'user' 
                ? "bg-blue-600 text-white rounded-tr-none" 
                : "bg-slate-50 dark:bg-slate-800 dark:text-slate-100 rounded-tl-none border border-slate-100 dark:border-slate-700"
            )}>
              {msg.status === 'error' && <AlertCircle className="inline-block mr-2 h-4 w-4 text-red-400" />}
              <Markdown content={msg.content} />
              {msg.status === 'generating' && (
                <span className="inline-block w-1 h-4 ml-1 bg-blue-400 animate-pulse align-middle" />
              )}
            </div>
          </div>
        ))}
      </div>

      {/* 输入区 */}
      <div className="p-6 bg-slate-50/50 dark:bg-slate-800/50 border-t border-slate-100 dark:border-slate-700">
        <div className="flex gap-3 max-w-4xl mx-auto relative">
          <Input 
            value={input}
            onChange={(e: any) => setInput(e.target.value)}
            onKeyDown={(e: any) => e.key === 'Enter' && !e.shiftKey && handleSend()}
            placeholder="输入您的问题 (Shift + Enter 换行)"
            className="flex-1 min-h-[50px] py-3 pr-12 resize-none bg-white dark:bg-slate-900 border-slate-200"
          />
          <Button 
            onClick={handleSend}
            disabled={isLoading || !input.trim()}
            className="absolute right-2 top-2 h-9 w-9 p-0 bg-blue-600 hover:bg-blue-700 rounded-full transition-all active:scale-95"
          >
            {isLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
          </Button>
        </div>
        <p className="text-[10px] text-center mt-3 text-muted-foreground">
          基于 Enterprise AI Starter 高可用网关 · 响应已由 Resilience4j 和语义缓存保护
        </p>
      </div>
    </Card>
  );
}
