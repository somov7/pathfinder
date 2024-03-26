import React, {useEffect, useState} from "react";
import {TextField} from "@mui/material";
import IconButton from '@mui/material/IconButton';
import SearchIcon from '@mui/icons-material/Search';
import axios from 'axios';
import Suggestions from "./Suggestions";
import {LocationPoint} from "./SideBar";

export type Suggestion = {
    title: string,
    address: string,
    uri: string
}

type AddLocationProps = {
    addLocation: (loc: LocationPoint) => void
}

const AddLocation = ({addLocation}: AddLocationProps) => {
    const [query, setQuery] = useState<string>("")
    const [suggestions, setSuggestions] = useState<Suggestion[] | undefined>(undefined)

    const searchLocation = (q: string) => {
        if (!q) {
            setSuggestions(undefined)
            return
        }
        axios.get<any>(process.env.REACT_APP_YANDEX_API_GEOSUGGEST_URL!,
            {
                params: {
                    apikey: process.env.REACT_APP_YANDEX_GEOSUGGEST_API_KEY,
                    text: q,
                    attrs: 'uri'
                }
            }
        )
            .then(response => {
                setSuggestions(response.data.results.map((res: any) => {
                    return {
                        title: res.title.text,
                        address: res.subtitle.text,
                        uri: res.uri
                    } as Suggestion
                }))
            })
    }

    return (
        <>
            <TextField
                label="Введите точку"
                variant="filled"
                onChange = {(e) => {
                    setQuery(e.target.value)
                    setSuggestions(undefined)
                }}
                onKeyPress = {(e) => {
                    if(e.key === 'Enter'){
                        searchLocation(query)
                    }
                }}
            >
            </TextField>
            <IconButton onClick={(e) => searchLocation(query)}>
                <SearchIcon/>
            </IconButton>
            {suggestions &&
              <Suggestions suggestions={suggestions} setSuggestions={setSuggestions} addLocation={addLocation}/>
            }
        </>
    );
}

export default AddLocation