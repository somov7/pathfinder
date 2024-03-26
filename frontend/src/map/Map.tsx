import React, {useEffect, useState} from "react";
import ReactDOM from "react-dom";
import {LocationPoint} from "../sidebar/SideBar";
import PlaceIcon from '@mui/icons-material/Place';

const ymaps3Reactify = await ymaps3.import('@yandex/ymaps3-reactify');
const reactify = ymaps3Reactify.reactify.bindTo(React, ReactDOM);
const {YMap, YMapDefaultSchemeLayer, YMapDefaultFeaturesLayer, YMapMarker} = reactify.module(ymaps3);
const {YMapDefaultMarker} = reactify.module(await ymaps3.import('@yandex/ymaps3-markers@0.0.1'));

type MapProps = {
    locations: LocationPoint[]
}

const Map = ({locations}: MapProps) => {
    const [center, setCenter] = useState<[number, number]>([30.268510, 60.0193821])

    useEffect(() => {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
                (pos) => {
                    setCenter([pos.coords.longitude, pos.coords.latitude])
                }
            );
        }
    }, []);

    console.log({locations, center})

    return (
        <div className="Map">
            <YMap location={{center: center, zoom: 11}} mode="vector">
                <YMapDefaultSchemeLayer />
                <YMapDefaultFeaturesLayer />
                {
                    locations.map(loc => (
                        <YMapDefaultMarker
                            coordinates={[loc.longitude, loc.latitude]}
                            title={loc.title}
                        />
                    ))
                }
            </YMap>
        </div>
    )
}

export default Map;