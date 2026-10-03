import { Navigate, Route, Routes } from "react-router-dom";

import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import VerifyOtpPage from "./pages/VerifyOtpPage";
import DocumentsPage from "./pages/DocumentsPage";
import ChatPage from "./pages/ChatPage";
import ProtectedRoute from "./components/ProtectedRoute";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verify" element={<VerifyOtpPage />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/documents" element={<DocumentsPage />} />

        <Route path="/chat" element={<ChatPage />} />
      </Route>

      <Route path="/" element={<Navigate to="/documents" replace />} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
