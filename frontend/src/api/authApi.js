import apiClient from "./apiClient";

export const register = async (name, email, password) => {
  const response = await apiClient.post("/auth/register", {
    name,
    email,
    password,
  });

  return response.data;
};

export const verifyEmail = async (email, otp) => {
  await apiClient.post("/auth/verify-email", {
    email,
    otp,
  });
};

export const resendOtp = async (email) => {
  await apiClient.post("/auth/resend-otp", {
    email,
  });
};

export const login = async (email, password) => {
  const response = await apiClient.post("/auth/login", {
    email,
    password,
  });

  return response.data;
};
