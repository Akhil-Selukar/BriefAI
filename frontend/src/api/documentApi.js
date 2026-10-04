import apiClient from "./apiClient";

export const getDocuments = async () => {
  const response = await apiClient.get("/documents");
  return response.data;
};

export const uploadDocument = async (file) => {
  const formData = new FormData();
  formData.append("file", file);

  const response = await apiClient.post("/documents", formData);

  return response.data;
};

export const processDocument = async (documentId) => {
  const response = await apiClient.post(`/documents/${documentId}/process`);

  return response.data;
};

export const deleteDocument = async (documentId) => {
  await apiClient.delete(`/documents/${documentId}`);
};
