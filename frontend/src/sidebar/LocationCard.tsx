import React from "react";
import IconButton from '@mui/material/IconButton';
import {List, ListItem, ListItemText} from "@mui/material";
import ClearIcon from "@mui/icons-material/Clear";

type LocationCardProps = {
    title: string,
    address: string,
    key: string,
    deleteLocation: () => void
}

const LocationCard = ({title, address, key, deleteLocation}: LocationCardProps) => {
    return (
        <ListItem
            secondaryAction={
                <IconButton
                    edge="end"
                    aria-label="delete"
                    onClick={(_) => deleteLocation()}
                >
                    <ClearIcon/>
                </IconButton>
            }
        >
            <ListItemText
                primary={title}
                secondary={address}
            />
        </ListItem>
    );
}

export default LocationCard