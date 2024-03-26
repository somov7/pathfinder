export const removeElement = <T>(a: T[], predicate: (el: T) => boolean): T[] => {
    const copy = a.slice()
    return copy.filter(e => !predicate(e))
}