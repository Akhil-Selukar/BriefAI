import apiClient from "./apiClient";

export const getConversations = async () => {
  const response = await apiClient.get("/conversations");
  return response.data;
};

export const createConversation = async (title) => {
  const response = await apiClient.post("/conversations", {
    title,
  });

  return response.data;
};

export const getConversationMessages = async (conversationId) => {
  const response = await apiClient.get(
    `/conversations/${conversationId}/messages`,
  );

  return response.data;
};

export const deleteConversation = async (conversationId) => {
  await apiClient.delete(`/conversations/${conversationId}`);
};
