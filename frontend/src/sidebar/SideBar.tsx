import {useState} from "react";
import LocationCard from './LocationCard'
import { removeElement } from "../util/array_utils";
import MenuSharpIcon from '@mui/icons-material/MenuSharp';
import AddLocation from './AddLocation'
import {Button} from "@mui/material";
import axios from "axios";

export type LocationPoint = {
    title: string,
    address: string,
    uri: string,
    key: string,
    longitude: number,
    latitude: number
}

type SideBarProps = {
    locations: LocationPoint[],
    setLocations:  React.Dispatch<React.SetStateAction<LocationPoint[]>>
}

const SideBar = ({locations, setLocations}: SideBarProps) => {
    const [closed, setClosed] = useState<boolean>(false)

    const handleTspSearch = (locations: LocationPoint[]) => {
        axios.post(process.env.REACT_APP_SERVER_URL + "/find-path/locations",
            {
                locations: locations.map((l) => {
                    return {
                        longitude: l.longitude,
                        latitude: l.latitude,
                        title: l.title,
                        id: l.key
                    }
                }),
                cycle: false
            }
        ).then(response => {
            const path: string[] = response.data.path.map((id: string) => locations.find(l => l.key === id)).map((l: LocationPoint) => l.title);
            console.log({locations, response})
            console.log(path)
            alert(path.join(", "))
        })
    }

    let classes = 'SideBar'
    if (closed) {
        classes += ' closed'
    }

    return (
      <div className={classes}>
        <button
            className="SideBarCloseButton"
            onClick = {(e) => setClosed(closed => !closed)}
        >
            <MenuSharpIcon/>
        </button>
        {!closed && (
            <div>
              <AddLocation addLocation={(loc: LocationPoint) => setLocations(prev => {
                  if (prev) {
                      const copy = prev.slice()
                      copy.push(loc)
                      return copy
                  }
                  return [loc]
              })}
              />
              {
                  locations.map(l => (
                      <LocationCard
                          title={l.title}
                          address={l.address}
                          key={l.key}
                          deleteLocation={() => setLocations((ls: LocationPoint[]) => removeElement(ls, el => el.key === l.key))}
                      />
                  ))
              }
            </div>
        )}
      <Button
          onClick={(_) => handleTspSearch(locations)}
      >Построить маршрут</Button>
      </div>
    )
}

export default SideBar;
