package ar.edu.utn.cajasliterarias.backend.exception;

/** Ya existe una cuenta (de suscriptor o de administradora) con ese email. Se responde 409. */
public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException() {
        super("Ya existe una cuenta con ese email.");
    }
}