function similarityPercent(similarity) {
  if (similarity == null || !Number.isFinite(similarity)) {
    return null;
  }

  return Math.round(similarity * 100);
}

function getCitedSourceNumbers(answer) {
  const citedNumbers = new Set();

  if (!answer) {
    return citedNumbers;
  }

  const citationPattern = /\[(\d+)\]/g;
  let match;

  while ((match = citationPattern.exec(answer)) !== null) {
    citedNumbers.add(Number(match[1]));
  }

  return citedNumbers;
}

export default function SourceCards({ sources = [], answer = "" }) {
  const citedNumbers = getCitedSourceNumbers(answer);

  const citedSources = sources.filter((source) =>
    citedNumbers.has(source.sourceNumber),
  );

  if (!citedSources.length) {
    return null;
  }

  return (
    <div className="message-sources">
      <div className="sources-heading">Cited sources</div>

      <div className="source-list">
        {citedSources.map((source) => {
          const score = similarityPercent(source.similarity);

          return (
            <div
              className="source-card"
              key={`${source.sourceNumber}-${source.chunkIndex}`}
            >
              <div className="source-number">{source.sourceNumber}</div>

              <div className="source-details">
                <div className="source-name">{source.documentName}</div>

                <div className="source-meta">
                  {source.pageNumber != null ? (
                    <span>Page {source.pageNumber}</span>
                  ) : (
                    <span>Document excerpt</span>
                  )}

                  {score != null && (
                    <>
                      <span>•</span>
                      <span>{score}% match</span>
                    </>
                  )}

                  {source.documentId == null && (
                    <>
                      <span>•</span>
                      <span>Archived source</span>
                    </>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
