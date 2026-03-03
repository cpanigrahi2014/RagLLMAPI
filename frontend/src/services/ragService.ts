import api from './api';
import type {
  ApiResponse,
  QueryRequest,
  QueryResponse,
  UsageStats,
  Book,
  BookUploadResponse,
  DashboardMetrics,
  UserUsage,
  AvailableModels,
  QueryTrend,
  TopQuery,
  Invoice,
  CurrentCost,
  CustomPromptRequest,
  SummarizeRequest,
  GenerateQARequest,
  TTSRequest,
  StudioResponse,
} from '@/types';

export const ragService = {
  // Query
  askQuestion: async (data: QueryRequest): Promise<QueryResponse> => {
    const res = await api.post<ApiResponse<QueryResponse>>('/query', data);
    return res.data.data;
  },

  // Available AI Models
  getModels: async (): Promise<AvailableModels> => {
    const res = await api.get<ApiResponse<AvailableModels>>('/models');
    return res.data.data;
  },

  getUsage: async (): Promise<UsageStats> => {
    const res = await api.get<ApiResponse<UsageStats>>('/query/usage');
    return res.data.data;
  },

  // Documents
  uploadBook: async (formData: FormData): Promise<BookUploadResponse> => {
    const res = await api.post<ApiResponse<BookUploadResponse>>('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
    });
    return res.data.data;
  },

  getBooks: async (): Promise<Book[]> => {
    const res = await api.get<ApiResponse<Book[]>>('/documents/books');
    return res.data.data;
  },

  getBook: async (bookId: string): Promise<Book> => {
    const res = await api.get<ApiResponse<Book>>(`/documents/books/${bookId}`);
    return res.data.data;
  },

  deleteBook: async (bookId: string): Promise<void> => {
    await api.delete(`/documents/books/${bookId}`);
  },

  // Analytics
  getDashboard: async (): Promise<DashboardMetrics> => {
    const res = await api.get<ApiResponse<DashboardMetrics>>('/analytics/dashboard');
    return res.data.data;
  },

  getUserUsage: async (): Promise<UserUsage[]> => {
    const res = await api.get<ApiResponse<UserUsage[]>>('/analytics/users');
    return res.data.data;
  },

  getQueryTrend: async (): Promise<QueryTrend[]> => {
    const res = await api.get<ApiResponse<QueryTrend[]>>('/analytics/trend');
    return res.data.data;
  },

  getTopQueries: async (): Promise<TopQuery[]> => {
    const res = await api.get<ApiResponse<TopQuery[]>>('/analytics/top-queries');
    return res.data.data;
  },

  // Billing
  getInvoices: async (): Promise<Invoice[]> => {
    const res = await api.get<ApiResponse<Invoice[]>>('/billing/invoices');
    return res.data.data;
  },

  getCurrentCost: async (): Promise<CurrentCost> => {
    const res = await api.get<ApiResponse<CurrentCost>>('/billing/current-cost');
    return res.data.data;
  },

  upgradePlan: async (plan: string): Promise<void> => {
    await api.post('/billing/upgrade', { plan });
  },

  // ═══ AI Studio ═══

  studioCustomPrompt: async (data: CustomPromptRequest): Promise<StudioResponse> => {
    const res = await api.post<ApiResponse<StudioResponse>>('/studio/prompt', data);
    return res.data.data;
  },

  studioSummarize: async (data: SummarizeRequest): Promise<StudioResponse> => {
    const res = await api.post<ApiResponse<StudioResponse>>('/studio/summarize', data);
    return res.data.data;
  },

  studioGenerateQA: async (data: GenerateQARequest): Promise<StudioResponse> => {
    const res = await api.post<ApiResponse<StudioResponse>>('/studio/generate-qa', data);
    return res.data.data;
  },

  studioTTS: async (data: TTSRequest): Promise<Blob> => {
    const res = await api.post('/studio/tts', data, { responseType: 'blob' });
    return res.data;
  },
};
