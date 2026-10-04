import apiClient from "./apiClient";

export const askQuestion = async (conversationId, question) => {
  const response = await apiClient.post("/chat/ask", {
    conversationId,
    question,
  });

  return response.data;
};
