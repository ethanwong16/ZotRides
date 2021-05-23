import {useState, useEffect} from 'react';
import {Button, Col, Row, Jumbotron, Container} from 'react-bootstrap';
import {Typeahead} from "react-bootstrap-typeahead";
import './stylesheets/Home.css';
import jQuery from "jquery";
import HOST from "../Host";
import {Redirect} from "react-router";

export default function Home() {
    const [singleSelections, setSingleSelections] = useState([]);
    const [text, setText] = useState("");
    const [options, setOptions] = useState([]);
    const [success, setSuccess] = useState(false);
    const [redirectId, setRedirectId] = useState();
    const [redirect, setRedirect] = useState(false);
    const [cache, setCache] = useState({});
    const [activeIndex, setActiveIndex] = useState(-1);

    useEffect(() => {
        const timeOutId = setTimeout(() => runQuery(text), 300);
        return () => clearTimeout(timeOutId);
    }, [text]);

    function runQuery(input) {
        var match = "";
        if(input.length >= 3 && Object.keys(cache).some((element) => {
            match = element;
            return ~element.indexOf(input.toLowerCase());
        })) {
            console.log("Autocomplete options populated from cache");
            setOptions(cache[match]);
            console.log("CACHE CONTENTS:");
            console.log(cache);
        } else if(input.length >= 3) {
            console.log("Autocomplete options populated from database");
            jQuery.ajax({
                dataType: "json",
                method: "POST",
                data: {token: input.toLowerCase()},
                url: HOST + "api/autocomplete",
                success: (resultData) => {
                    setOptions(resultData.results);
                    const temp = cache;
                    temp[input] = resultData.results;
                    console.log("Added to cache")
                    setCache(temp);
                    console.log("CACHE CONTENTS:");
                    console.log(cache);
                }
            });
        } else {
            setOptions([]);
        }
    }

    function onChange(input) {
        setText(input.toLowerCase());
    }
    function onSelect(input) {
        setSingleSelections(input);
        options.map(element => {
            if(input[0] === element["car_name"])
                setRedirectId(element["car_id"]);
        })
        setSuccess(true);
    }

    function handleKey(e) {
        if (e.key === "Enter" && activeIndex === -1) {
            setRedirect(true);
        }
    }

    if(redirect)
        return (<Redirect to={"/browse/na/na/na/na/na/na/" + text}/>);
    if(success)
        return (<Redirect to={"/cars/" + redirectId}  />)
    return (
        <div>
            <Jumbotron className={"hero-section"}>
                <Container>
                    <h1>Welcome to ZotRides</h1>
                    <p className={"subtitle"}>Book unforgettable cars from locations around the world</p>
                    <Row>
                        <Col>
                            <Typeahead
                                onInputChange={onChange}
                                delay={300}
                                id="basic-typeahead-single"
                                labelKey="name"
                                onChange={onSelect}
                                options={options.map((element => {return element["car_name"]}))}
                                placeholder="Enter model name..."
                                selected={singleSelections}
                                onKeyDown={handleKey}
                            >
                                {(state) => {
                                    // Passing a child render function to the component exposes partial
                                    // internal state, including the index of the highlighted menu item.
                                    setActiveIndex(state.activeIndex);
                                }}
                            </Typeahead>
                        </Col>
                        <Col xs={1}>
                            <Button onClick={()=>{if(text.length !== 0) setRedirect(true)}}>Search</Button>
                        </Col>
                    </Row>
                </Container>
            </Jumbotron>
        </div>
    );
}