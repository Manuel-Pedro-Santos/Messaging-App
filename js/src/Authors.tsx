import * as React from "react";
import { Link } from "react-router-dom";
import 'bootstrap/dist/css/bootstrap.min.css';

export function Authors() {
    return (
        <div className="container mt-5 text-center">
            <h1 className="mb-5">Authors</h1>
            <div className="row">
                <div className="col-md-4 mb-4">
                    <div className="card">
                        <img src="/images/image_manuel.jpeg" className="card-img-top" alt="Manuel Santos" />
                        <div className="card-body">
                            <h5 className="card-title">Manuel Santos</h5>
                        </div>
                    </div>
                </div>
                <div className="col-md-4 mb-4">
                    <div className="card">
                        <img src="/images/image_tomas.jpeg" className="card-img-top" alt="Tomás Mendes" />
                        <div className="card-body">
                            <h5 className="card-title">Tomás Mendes</h5>
                        </div>
                    </div>
                </div>
                <div className="col-md-4 mb-4">
                    <div className="card">
                        <img src="/images/image_pedro.jpeg" className="card-img-top" alt="Pedro Silva" />
                        <div className="card-body">
                            <h5 className="card-title">Pedro Silva</h5>
                        </div>
                    </div>
                </div>
            </div>
            <Link to="/">
                <button className="btn btn-primary mt-4">Go Back</button>
            </Link>
        </div>
    );
}