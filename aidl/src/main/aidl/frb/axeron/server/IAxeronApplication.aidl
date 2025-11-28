package frb.axeron.server;

interface IAxeronApplication {

    oneway void bindApplication(in Bundle data) = 1;
}