import { Response } from "express";
import { ApiErrorResponse, ErrorCode } from "../models/index.js";

export class AppError extends Error {
  public readonly statusCode: number;
  public readonly code: ErrorCode;

  constructor(statusCode: number, code: ErrorCode, message: string) {
    super(message);
    this.statusCode = statusCode;
    this.code = code;
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

export function sendError(res: Response, statusCode: number, code: ErrorCode, message: string): void {
  const payload: ApiErrorResponse = {
    status: "error",
    code,
    message,
  };
  res.status(statusCode).json(payload);
}
