import { Formik } from 'formik';
import * as yup from 'yup';
import { Form, Button } from 'react-bootstrap';

export default function(props) {
    return (
        <Formik
            initialValues={{ username: "", password: ""}}
            onSubmit={(async (values) => {
                // props.setLoading(true);
                console.log(values);
            })}
        >
            {({
                  handleSubmit,
                  handleChange,
                  handleBlur,
                  values,
                  touched,
                  errors,
              }) => (
                <Form noValidate onSubmit={handleSubmit}>
                    <Form.Group>
                        <Form.Label>Username</Form.Label>
                        <Form.Control
                            type={"string"}
                            name={"username"}
                            id={"username"}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            value={values.username}
                            placeholder={"Enter username"}
                            isInvalid={touched.username && errors.username}
                        />
                        <Form.Control.Feedback type={"invalid"}>
                            {errors.username}
                        </Form.Control.Feedback>
                    </Form.Group>
                    <Form.Group>
                        <Form.Label>Password</Form.Label>
                        <Form.Control
                            type={"password"}
                            name={"password"}
                            id={"password"}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            value={values.password}
                            placeholder={"Enter password"}
                            isInvalid={touched.password && errors.password}
                        />
                        <Form.Control.Feedback type={"invalid"}>
                            {errors.password}
                        </Form.Control.Feedback>
                    </Form.Group>
                    <Button variant={"primary"} type={"submit"}>Login</Button>
                    <Button variant={"link"} value={"register"} onClick={props.switchForm}>Create an Account</Button>
                </Form>
            )}
        </Formik>
    )
}