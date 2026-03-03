export type Role = 'ADMIN' | 'TEACHER' | 'STUDENT';

export interface User {
  userId: string;
  email: string;
  fullName: string;
  role: Role;
  tenantId: string;
  tenantName: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  role: Role;
  email: string;
  tenantId: string;
  tenantName?: string;
  userId: string;
  fullName?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  tenantName: string;
  role: Role;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  error?: string;
  timestamp: string;
}

export interface Book {
  id: string;
  name: string;
  subject: string;
  classLevel: number;
  processingStatus: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  totalPages?: number;
  filePath?: string;
  tenantId: string;
  createdAt: string;
  updatedAt?: string;
}

export interface BookUploadResponse {
  bookId: string;
  name: string;
  status: string;
  message: string;
}

export interface QueryRequest {
  query: string;
  subject?: string;
  classLevel?: number;
  maxResults?: number;
  chatModel?: string;
  embeddingModel?: string;
  useRag?: boolean;
}

export interface QuerySource {
  chunkId: string;
  content: string;
  bookName: string;
  chapterTitle: string;
  pageNumber: number;
  similarityScore: number;
}

export interface QueryResponse {
  answer: string;
  sources: QuerySource[];
  tokensUsed: number;
  responseTimeMs: number;
  chatModel?: string;
  embeddingModel?: string;
}

export interface UsageStats {
  tenantId: string;
  tenantName: string;
  subscriptionPlan: string;
  totalTokensUsed: number;
  monthlyTokenLimit: number;
  remainingTokens: number;
  usagePercentage: number;
  estimatedCostUsd: number;
}

export interface DashboardMetrics {
  totalQueries: number;
  totalDocuments: number;
  totalUsers: number;
  averageResponseTime: number;
  tokensUsedToday: number;
  queriesThisWeek: number;
}

export interface UserUsage {
  userId: string;
  fullName: string;
  email: string;
  role: Role;
  totalQueries: number;
  tokensUsed: number;
  lastActivityAt: string;
}

export interface QueryTrend {
  date: string;
  queryCount: number;
  tokensUsed: number;
}

export interface TopQuery {
  query: string;
  count: number;
  subject: string;
}

export interface Invoice {
  invoiceId: string;
  period: string;
  amount: number;
  status: string;
  createdAt: string;
}

export interface CurrentCost {
  currentPeriod: string;
  estimatedCost: number;
  tokensUsed: number;
  plan: string;
}

export type SubscriptionPlan = 'FREE' | 'BASIC' | 'STANDARD' | 'PREMIUM' | 'ENTERPRISE';

// AI Model types
export interface ChatModelInfo {
  id: string;
  name: string;
  description: string;
  tier: string;
  provider?: string;
}

export interface EmbeddingModelInfo {
  id: string;
  name: string;
  description: string;
  dimensions: number;
  provider?: string;
}

export interface AvailableModels {
  chatModels: ChatModelInfo[];
  embeddingModels: EmbeddingModelInfo[];
  defaults: {
    chatModel: string;
    embeddingModel: string;
  };
}

// ═══ AI Studio Types ═══

export interface CustomPromptRequest {
  prompt: string;
  bookId?: string;
  chatModel?: string;
  embeddingModel?: string;
  useDocumentContext?: boolean;
}

export interface SummarizeRequest {
  bookId: string;
  chatModel?: string;
  style?: 'brief' | 'detailed' | 'bullet-points';
  embeddingModel?: string;
}

export interface GenerateQARequest {
  bookId: string;
  chatModel?: string;
  count?: number;
  difficulty?: 'easy' | 'medium' | 'hard' | 'mixed';
  embeddingModel?: string;
}

export interface TTSRequest {
  text: string;
  voice?: 'alloy' | 'echo' | 'fable' | 'onyx' | 'nova' | 'shimmer';
  model?: 'tts-1' | 'tts-1-hd';
  speed?: number;
}

export interface QAPair {
  number: number;
  question: string;
  answer: string;
  difficulty: string;
}

export interface StudioResponse {
  result: string;
  tokensUsed: number;
  responseTimeMs: number;
  chatModel: string;
  type: string;
  qaPairs?: QAPair[];
}
