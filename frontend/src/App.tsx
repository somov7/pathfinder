import React, {useState} from 'react';
import './App.css';
import Map from "./map/Map";
import SideBar, {LocationPoint} from './sidebar/SideBar';

function App() {
  const [locations, setLocations] = useState<LocationPoint[]>([])

  return (
    <div className="App">
      <SideBar locations={locations} setLocations={setLocations}/>
      <Map locations={locations}/>
    </div>
  );
}

export default App;
