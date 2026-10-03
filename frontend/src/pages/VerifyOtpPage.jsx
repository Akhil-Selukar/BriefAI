import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import { resendOtp, verifyEmail } from "../api/authApi";

import { getApiErrorMessage } from "../api/apiError";

export default function VerifyOtpPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const email = location.state?.email || "";

  const [otp, setOtp] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState(location.state?.message || "");
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();

    setError("");
    setMessage("");

    if (!/^\d{6}$/.test(otp)) {
      setError("Enter the 6-digit verification code.");
      return;
    }

    try {
      setLoading(true);

      await verifyEmail(email, otp);

      navigate("/login", {
        replace: true,
        state: {
          message: "Email verified successfully. You can now sign in.",
          email,
        },
      });
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setError("");
    setMessage("");

    try {
      setResending(true);

      await resendOtp(email);

      setMessage("A new verification code has been sent.");
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setResending(false);
    }
  };

  if (!email) {
    return (
      <div className="auth-page">
        <div className="auth-brand">
          <div className="brand-mark">B</div>
          <span>BriefAI</span>
        </div>

        <main className="auth-card">
          <div className="auth-heading">
            <h1>Verification session expired</h1>
            <p>Please register again to verify your email address.</p>
          </div>

          <Link className="primary-button button-link" to="/register">
            Back to registration
          </Link>
        </main>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-brand">
        <div className="brand-mark">B</div>
        <span>BriefAI</span>
      </div>

      <main className="auth-card">
        <div className="auth-heading">
          <h1>Check your email</h1>

          <p>
            We sent a 6-digit verification code to <strong>{email}</strong>.
          </p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        {message && <div className="alert alert-success">{message}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="otp">Verification code</label>

            <input
              id="otp"
              className="otp-input"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              value={otp}
              onChange={(event) => {
                const value = event.target.value.replace(/\D/g, "");

                setOtp(value);
              }}
              placeholder="000000"
              required
            />
          </div>

          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? "Verifying..." : "Verify email"}
          </button>
        </form>

        <div className="resend-section">
          <span>Didn't receive the code?</span>

          <button
            className="text-button"
            type="button"
            onClick={handleResend}
            disabled={resending}
          >
            {resending ? "Sending..." : "Resend code"}
          </button>
        </div>

        <p className="auth-footer">
          <Link to="/register">Use a different email</Link>
        </p>
      </main>
    </div>
  );
}
