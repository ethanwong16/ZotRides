import {Card, Col} from "react-bootstrap";
import React from "react";
import UpdateModal from "./CartItem/UpdateModal";

export default function(props) {
    return (
        <Col xs={4}>
            <UpdateModal />
            <Card className={'text-center'}>
                <Card.Img
                    className={'mx-auto'}
                    variant={'top'}
                    src={'https://d1zgdcrdir5wgt.cloudfront.net/media/vehicle/images/-GWIN1aUTRO8HI2WwVEzIA.1440x700.jpg'}
                />
                <Card.Body className={'pt-1'}>
                    <Card.Title style={{fontSize: '30px'}} className={'mb-1'}>
                        {props.name}
                    </Card.Title>
                    <p>Price: {props.price}</p>
                    <p>Pickup Location: {props.location}</p>
                    <p>Start Date: {props.start}</p>
                    <p>End Date: {props.end}</p>
                </Card.Body>

            </Card>
        </Col>
    )
}