export class AppError extends Error {
    statusCode;
    code;
    constructor(statusCode, code, message) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
        Object.setPrototypeOf(this, new.target.prototype);
    }
}
export function sendError(res, statusCode, code, message) {
    const payload = {
        status: "error",
        code,
        message,
    };
    res.status(statusCode).json(payload);
}
