export const getEulerianCycle = (n: number, batchSize: number | null): number[][] => {
    const path: number[] = []
    for (let i = 0; i < n; i++) {
        for (let j = i + 1; j < n; j++) {
            path.push(i, j)
        }
    }
    path.push(0)
    if (!batchSize) {
        return [path]
    }
    let chunks: number[][] = []
    for (let i = 0; i < path.length; i += batchSize) {
        const slice = path.slice(i, i + batchSize + 1);
        if (slice.length > 1) {
            chunks.push(slice)
        }
    }
    return chunks
}
