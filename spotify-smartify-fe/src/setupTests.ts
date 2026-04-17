// jest-dom adds custom jest matchers for asserting on DOM nodes.
import '@testing-library/jest-dom';

// react-router v7 uses TextEncoder/TextDecoder which are not present in jest's
// default jsdom environment (Node <18 or older jsdom versions). Polyfill them.
import { TextEncoder, TextDecoder } from 'util';
Object.assign(global, { TextEncoder, TextDecoder });
