export function getApiErrorMessage(error) {
  const data = error.response?.data;

  if (!data) {
    return "Unable to connect to the server. Please try again.";
  }

  if (typeof data === "string") {
    return data;
  }

  return (
    data.message || data.error || "Something went wrong. Please try again."
  );
}
