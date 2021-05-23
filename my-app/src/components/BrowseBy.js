import {Col, Row, Container, Button} from "react-bootstrap";
import {useEffect, useState} from "react";
import jQuery from "jquery";
import HOST from "../Host";
import {Redirect} from "react-router";
import SearchForm from "./BrowseBy/SearchForm";

export default function() {
    const [categories, setCategories] = useState();
    const [loading, setLoading] = useState(true);
    const [redirect, setRedirect] = useState(false);
    const [redirectURL, setRedirectURL] = useState("");

    useEffect(() => {
        console.log("Asdfasdf")
        jQuery.ajax({
            dataType: "json",
            method: "GET",
            url: HOST + "api/categories",
            success: (resultData) => {
                console.log(resultData);
                setCategories(resultData.map((element) => {
                    return element.category
                }));
                setLoading(false);
            }
        });
    }, []);

    function handleClick(event) {
        setRedirectURL("/browse/na/na/na/na/" + event.target.id);
        setRedirect(true);
    }

    if(loading) return <div>LOADING</div>
    if(redirect) return <Redirect to={redirectURL}/>

    return (
        <Container>
            <Row>
                <Col>
                    <h1> Search </h1>
                    <SearchForm />
                </Col>
                <Col>
                    <h1> Browse by Vehicle Type </h1>
                    {
                        categories.map((category) => (
                            <Button id={category} onClick={handleClick} variant={"link"}>{category}</Button>
                        ))
                    }
                </Col>
                <Col>
                    <h1> Browse by Vehicle Name </h1>
                    {
                        letters.map((category, index) => (
                            <Button id={category} key={index} onClick={handleClick} variant={"link"}>{category}</Button>
                        ))
                    }
                </Col>
            </Row>
        </Container>
    )
}

const letters = ["A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z", "1","2","3","4","5","6","7","8","9","*"]