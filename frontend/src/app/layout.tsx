import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { 
  LayoutDashboard, 
  Settings, 
  Layers, 
  Key, 
  Database,
  Menu,
  MessageSquare
} from 'lucide-react';
import Link from 'next/link';

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "Enterprise Java-AI Dashboard",
  description: "Next-generation AI Infrastructure management",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const navItems = [
    { label: '概览仪表盘', icon: LayoutDashboard, href: '/dashboard' },
    { label: '智能对话', icon: MessageSquare, href: '/chat' },
    { label: '模型渠道', icon: Layers, href: '/dashboard/channels' },
    { label: '应用管理', icon: Key, href: '#' },
    { label: '语义缓存', icon: Database, href: '#' },
  ];

  return (
    <html lang="zh">
      <body className={inter.className}>
        <div className="flex min-h-screen bg-slate-50">
          {/* Sidebar */}
          <aside className="hidden md:flex w-64 flex-col border-r bg-white">
            <div className="p-6 flex items-center gap-2">
              <div className="h-8 w-8 rounded-lg bg-primary flex items-center justify-center">
                <span className="text-white font-bold text-xl">A</span>
              </div>
              <span className="font-bold text-xl tracking-tight">AI Starter</span>
            </div>
            <nav className="flex-1 px-4 space-y-1">
              {navItems.map((item) => (
                <Link
                  key={item.label}
                  href={item.href}
                  className="flex items-center gap-3 px-3 py-2 text-sm font-medium text-slate-600 rounded-lg hover:bg-slate-100 hover:text-slate-900 transition-colors"
                >
                  <item.icon className="h-4 w-4" />
                  {item.label}
                </Link>
              ))}
            </nav>
          </aside>

          {/* Main Content */}
          <main className="flex-1 flex flex-col">
            <header className="h-16 border-b bg-white flex items-center justify-between px-8">
               <div className="md:hidden">
                  <Menu className="h-6 w-6" />
               </div>
               <div className="flex items-center gap-4">
                  <div className="text-sm font-medium text-slate-500">
                     企业版本 v1.0.0
                  </div>
                  <div className="h-8 w-8 rounded-full bg-slate-200" />
               </div>
            </header>
            <div className="flex-1 overflow-auto">
              {children}
            </div>
          </main>
        </div>
      </body>
    </html>
  );
}
