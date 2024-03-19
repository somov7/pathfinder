import {useState} from "react";
import {Button, TextField} from "@mui/material";
import {getEulerianCycle} from "./util/euler_utils";

const EulerPathFinder = () => {
    const [counter, setCounter] = useState<number>(0)
    const [path, setPath] = useState<string>("")

    return (
        <>
            <TextField
                type="number"
                label="Количество вершин"
                variant="filled"
                onChange = {(e) => setCounter(+e.target.value)}
            >
            </TextField>
            <Button
                onClick={(e) => {
                    let p = getEulerianCycle(counter, 50)
                    setPath("Эйлеров цикл:\n" + p.map(q => '[' + q.join(' => ') + ']').join('\n'))
                }}
            >
                Показать эйлеров цикл
            </Button>
            <p className={"output"}>{path}</p>
        </>
    )
}

export default EulerPathFinder;