import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import { login } from "../api/authApi";
import { getApiErrorMessage } from "../api/apiError";

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState(location.state?.email || "");

  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const [message, setMessage] = useState(location.state?.message || "");

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (localStorage.getItem("briefai_token")) {
      navigate("/documents", {
        replace: true,
      });
    }
  }, [navigate]);

  const handleSubmit = async (event) => {
    event.preventDefault();

    setError("");
    setMessage("");

    try {
      setLoading(true);

      const response = await login(email.trim(), password);

      localStorage.setItem("briefai_token", response.accessToken);
      localStorage.setItem("briefai_user_email", email.trim());

      navigate("/documents", {
        replace: true,
      });
    } catch (err) {
      const errorCode = err.response?.data?.error;

      if (errorCode === "EMAIL_NOT_VERIFIED") {
        navigate("/verify", {
          state: {
            email: email.trim(),
            message:
              "Your email hasn't been verified yet. Enter your verification code or request a new one.",
          },
        });

        return;
      }

      setError(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-brand">
        <div className="brand-mark">B</div>
        <span>BriefAI</span>
      </div>

      <main className="auth-card">
        <div className="auth-heading">
          <h1>Welcome back</h1>

          <p>Sign in to continue working with your documents.</p>
        </div>

        {message && <div className="alert alert-success">{message}</div>}

        {error && <div className="alert alert-error">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="email">Email</label>

            <input
              id="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="sheldon.cooper@gmail.com"
              autoComplete="email"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>

            <input
              id="password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Enter your password"
              autoComplete="current-password"
              required
            />
          </div>

          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? "Signing in..." : "Sign in"}
          </button>
        </form>

        <p className="auth-footer">
          Don't have an account? <Link to="/register">Create account</Link>
        </p>
      </main>
    </div>
  );
}
