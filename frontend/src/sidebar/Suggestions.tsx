import React from "react";
import {List, ListItem, ListItemText, Box, Divider} from "@mui/material";
import {v4 as uuidv4} from 'uuid';
import {Suggestion} from './AddLocation'
import {LocationPoint} from './SideBar'
import axios from "axios";

type SuggestionsProps = {
    suggestions: Suggestion[],
    setSuggestions: (sug: Suggestion[] | undefined) => void
    addLocation: (loc: LocationPoint) => void
}

const Suggestions = ({suggestions, setSuggestions, addLocation}: SuggestionsProps) => {

    return (
        <Box onBlur={(e) => setSuggestions(undefined)} className="Suggestions">
            <List dense={true}>
            {
                suggestions.map(sug =>
                    <ListItem
                        onClick={(e) => {
                            axios.get(process.env.REACT_APP_YANDEX_API_GEOCODER_URL!, {
                                    params: {
                                        apikey: process.env.REACT_APP_YANDEX_API_KEY,
                                        uri: sug.uri,
                                        lang: 'ru_RU',
                                        results: 1,
                                        format: 'json'
                                    }
                            })
                                .then(response => {
                                    const coords: string = response.data.response.GeoObjectCollection.featureMember[0].GeoObject.Point.pos
                                    const lonlat: number[] = coords.split(' ').map(c => +c)
                                    addLocation({
                                        title: sug.title,
                                        address: sug.address,
                                        uri: sug.uri,
                                        key: uuidv4(),
                                        longitude: lonlat[0],
                                        latitude: lonlat[1]
                                    })
                                })
                            setSuggestions(undefined)
                        }}
                    >
                        <ListItemText
                            primary={sug.title}
                            secondary={sug.address}
                        />
                        <Divider/>
                    </ListItem>
                )
            }
            </List>
        </Box>
    )
}

export default Suggestions