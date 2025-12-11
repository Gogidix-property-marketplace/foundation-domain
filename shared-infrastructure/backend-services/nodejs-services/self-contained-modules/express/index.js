const http = require('http');
const url = require('url');
const querystring = require('querystring');

function express() {
    const app = {
        _settings: {
            'etag': 'weak',
            'env': 'development',
            'query parser': { extended: true },
            'json spaces': 2,
            'views': 'views',
            'view engine': 'pug'
        },
        _router: {
            stack: [],
            params: {},
            _params: {}
        },
        _middleware: [],
        _routes: {
            get: [],
            post: [],
            put: [],
            delete: [],
            patch: [],
            options: [],
            head: []
        }
    };

    app.use = function(middleware) {
        this._middleware.push(middleware);
        return this;
    };

    // Route methods
    const addRoute = (method) => {
        return (path, ...handlers) => {
            if (typeof path === 'function') {
                handlers.unshift(path);
                path = '/';
            }
            this._routes[method].push({ path, handlers });
            return this;
        };
    };

    app.get = addRoute('get');
    app.post = addRoute('post');
    app.put = addRoute('put');
    app.delete = addRoute('delete');
    app.patch = addRoute('patch');
    app.options = addRoute('options');
    app.head = addRoute('head');

    // Router function
    app._router = function(req, res, next) {
        const method = req.method.toLowerCase();
        const pathname = url.parse(req.url).pathname;
        const routes = this._routes[method] || [];

        let matched = false;
        for (const route of routes) {
            if (route.path === pathname || route.path === '*') {
                matched = true;
                let i = 0;
                const executeHandler = () => {
                    if (i < route.handlers.length) {
                        route.handlers[i](req, res, executeHandler);
                        i++;
                    } else {
                        next();
                    }
                };
                executeHandler();
                break;
            }
        }

        if (!matched && !res.headersSent) {
            res.statusCode = 404;
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify({ error: 'Not Found' }));
        }
    };

    // Server creation
    app.listen = function(port, hostname, callback) {
        if (typeof hostname === 'function') {
            callback = hostname;
            hostname = '0.0.0.0';
        }

        const server = http.createServer((req, res) => {
            // Parse URL and query
            req.url = req.url || '/';
            req.method = req.method || 'GET';
            req.query = querystring.parse(url.parse(req.url).query);
            req.params = {};

            // Apply middleware and routing
            let middlewareIndex = 0;
            const next = () => {
                if (middlewareIndex < this._middleware.length) {
                    this._middleware[middlewareIndex++](req, res, next);
                } else {
                    this._router(req, res, () => {});
                }
            }.bind(this);

            next();
        }.bind(this));

        return server.listen(port, hostname, callback);
    };

    // Static files
    app.static = function(root, options = {}) {
        const fs = require('fs');
        const path = require('path');
        return (req, res, next) => {
            const filename = path.join(root, req.path.slice(1));
            if (fs.existsSync(filename) && fs.statSync(filename).isFile()) {
                const ext = path.extname(filename);
                const contentTypes = {
                    '.html': 'text/html',
                    '.js': 'text/javascript',
                    '.css': 'text/css',
                    '.json': 'application/json',
                    '.png': 'image/png',
                    '.jpg': 'image/jpeg',
                    '.gif': 'image/gif'
                };
                res.setHeader('Content-Type', contentTypes[ext] || 'text/plain');
                fs.createReadStream(filename).pipe(res);
            } else {
                next();
            }
        };
    };

    // Body parsers
    app.use(express.json());
    app.use(express.urlencoded({ extended: true }));

    // Router
    app.Router = function() {
        const router = {
            use: (middleware) => { return router; },
            get: (path, handler) => { return { path, handler }; },
            post: (path, handler) => { return { path, handler }; }
        };
        return router;
    };

    // JSON parser
    app.json = (options) => (req, res, next) => {
        next();
    };

    // URL encoded parser
    app.urlencoded = (options) => (req, res, next) => {
        next();
    };

    // Set engine
    app.set = function(setting, val) {
        this._settings[setting] = val;
        return this;
    };

    // Get engine
    app.get = function(setting) {
        return this._settings[setting];
    };

    return app;
}

module.exports = express;