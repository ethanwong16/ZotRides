import {useState} from 'react';
import {Nav, Navbar} from 'react-bootstrap';
import {LinkContainer} from 'react-router-bootstrap';
import SearchModal from "./Search/SearchModal";
import VehicleTypeModal from "./NavBarModals/VehicleTypeModal";

export default function MyNav(props) {
    const [modalShow, setModalShow] = useState(false);
    const [browseTypeModalShow, setBrowseTypeModalShow] = useState(false);
    const [browseLetterModalShow, setBrowseLetterModalShow] = useState(false);

    const handleClick = () => setModalShow(!modalShow);
    const handleBrowseType = () => {
        setBrowseTypeModalShow(!browseTypeModalShow);
    }
    return (
        <div>
            <VehicleTypeModal show={browseTypeModalShow}
                              onHide={() => setBrowseTypeModalShow(false)}
                              setModalShow={setBrowseTypeModalShow}
            />
            <SearchModal show={modalShow}
                         onHide={() => setModalShow(false)}
                         setModalShow={setModalShow}
            />
            <Navbar variant="light" className={"myNav"}>
                <LinkContainer to="/">
                    <Navbar.Brand>
                        <img
                            src={"https://i.ibb.co/hKLvRdN/Screen-Shot-2021-04-16-at-4-24-19-PM.png"}
                            style={{width: "145px"}}
                        />
                    </Navbar.Brand>
                </LinkContainer>
                <Navbar.Toggle aria-controls="basic-navbar-nav"/>
                <Navbar.Collapse id="basic-navbar-nav">
                    <Nav className={"mr-auto"}>
                        <LinkContainer to={"/browseby"}>
                            <Nav.Link>Make Query</Nav.Link>
                        </LinkContainer>
                    </Nav>
                    <Nav className={"ml-auto"}>
                        {/*// TODO: Connecting search with variable flag*/}
                        <LinkContainer to="/mycart">
                            <Nav.Link>My Cart</Nav.Link>
                        </LinkContainer>
                    </Nav>
                </Navbar.Collapse>
            </Navbar>
        </div>
    )
}