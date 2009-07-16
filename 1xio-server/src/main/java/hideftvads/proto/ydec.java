///**
// * ***********************************************************************************************
// * $Header: /pub/cvsroot/yencode/src/ydecode.c,v 1.27 2002/03/16 05:29:14 bboy Exp $
// * <p/>
// * Copyright (C) 2002  Don Moore <bboy@bboy.net>
// * <p/>
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 2 of the License, or
// * (at Your option) any later version.
// * <p/>
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// * <p/>
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
// * ************************************************************************************************
// */
//
////#include "y.h"
////#include "file.h"
//
//class ydec {
//    int opt_verbose = 1;                            /* Should the program be verbose in its operation? */
//    int opt_debug = 0;                                /* Debug output? */
//    int opt_overwrite = 0;                        /* Overwrite existing files? */
//    char*opt_output_dir=NULL;                    /* Write output to this directory */
//    int opt_recursive = 0;                        /* Recursive scan? */
//    int opt_scan = 0;                                /* Scan files? */
//    int opt_keep = 0;                                /* Keep output files with errors? */
//    int opt_keep_paths = 0;                        /* Keep paths in filenames? */
//    int opt_list = 0;                                /* Only list files found? */
//    int opt_test = 0;                                /* Test files? */
//    int opt_strict = 0;                            /* Strict test? */
//    int opt_delete = 0;                            /* Delete files after decoding? */
//
//    char**dirs=(char**)NULL;                    /* List of directories to process */
//    int num_dirs = 0;                                /* Number of directories in `dirs' list */
//
//    YDECFILE**yfiles=(YDECFILE**)NULL;            /* List of files to process */
//    int num_yfiles = 0;                            /* Number of items in yfiles */
//
///* Macro for passing filenames to usermsg() */
//    #
//
//    define YNAMES(y)
//
//    (y&&y->output_filename)?y->output_filename:
//
//    _("<UNKNOWN>"),
//
//    \
//            (y&&y->input_filename)?y->input_filename:
//
//    _("<UNKNOWN>")
//
//
//    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//        USAGE
//        Display program usage information.
//    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
//    void
//    usage(int status) {
////        if (status != EXIT_SUCCESS) {
////            fprintf(stderr, _("Try `%s --help' for more information."), progname);
////            fputs("\n", stderr);
////        } else {
////            printf(_("Usage: %s [OPTION]... [FILE]..."), progname);
////            puts("");
////            puts(_("Usenet file decoder."));
////            puts("");
//////		puts("----------------------------------------------------------------------------78");
////            puts(_("  -d, --debug       output extra debugging information while running"));
////            puts(_("  -D, --delete      delete input files if decoded successfully"));
////            puts(_("  -f, --force       overwrite existing files, never prompt"));
////            puts(_("  -k, --keep        keep output files that contain errors"));
////            puts(_("  -l, --list        find and list input file information only"));
////            puts(_("  -o, --output=DIR  create output in DIR instead of the current dir"));
////            puts(_("  -p, --paths       maintain paths in output filenames"));
////            puts(_("  -q, --quiet       inhibit all messages written to the standard output"));
////            puts(_("  -r, --recursive   scan directories recursively"));
////            puts(_("  -s, --scan        scan files if any parts are missing"));
////            puts(_("  -t, --test        do not write output files (just test archives)"));
////            puts(_("      --strict      perform strict format checks when testing/decoding?"));
////            puts(_("      --verify      synonym for `--test --strict'"));
////            puts(_("      --help        display this help and exit"));
////            puts(_("      --version     output version information and exit"));
////            puts("");
////            puts(_("Report bugs to bugs@yencode.org."));
////        }
//System.        exit(status);
//    }
///*--- usage() -----------------------------------------------------------------------------------*/
//
// 
///*--- queue_file() ------------------------------------------------------------------------------*/
//
//
//    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//        CMDLINE
//        Process command line options.
//    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
//    static void
//    cmdline(int argc, char**argv) {
//        int optc, optindex;
//        char*optstr;
//        struct option const longopts[]=
//        {
//            {
//                "debug", no_argument, NULL, 'd'
//            },
//            {
//                "delete", no_argument, NULL, 'D'
//            },
//            {
//                "force", no_argument, NULL, 'f'
//            },
//            {
//                "keep", no_argument, NULL, 'k'
//            },
//            {
//                "list", no_argument, NULL, 'l'
//            },
//            {
//                "output", required_argument, NULL, 'o'
//            },
//            {
//                "paths", no_argument, NULL, 'p'
//            },
//            {
//                "quiet", no_argument, NULL, 'q'
//            },
//            {
//                "recursive", no_argument, NULL, 'r'
//            },
//            {
//                "recurse", no_argument, NULL, 'R'
//            },
//            {
//                "scan", no_argument, NULL, 's'
//            },
//            {
//                "test", no_argument, NULL, 't'
//            },
//            {
//                "strict", no_argument, 0, 0
//            },
//            {
//                "verify", no_argument, 0, 0
//            },
//            {
//                "help", no_argument, 0, 0
//            },
//            {
//                "version", no_argument, 0, 0
//            },
//
//            {
//                NULL, 0, NULL, 0
//            }
//        }
//        ;
//        int file_errs = 0;
//
//        opt_verbose = 1;
//        optstr = getoptstr(longopts);
//        while ((optc = getopt_long(argc, argv, optstr, longopts, & optindex))!=-1)
//        {
//            switch (optc) {
//                case 0: {
//                    const char*opt = longopts[optindex].name;
//
//                    if (!strcmp(opt, "version"))                                    // --version
//                    {
//                        printf("%s - "PACKAGE" "VERSION"\n", short_progname);
//                        exit(EXIT_SUCCESS);
//                    } else if (!strcmp(opt, "help"))                                // --help
//                        usage(EXIT_SUCCESS);
//                    else if (!strcmp(opt, "strict"))                                // --strict
//                        opt_strict = 1;
//                    else if (!strcmp(opt, "verify"))                                // --verify
//                        opt_strict = opt_test = 1;
//                }
//                break;
//
//                case 'd':                                                                    // -d, --debug
//                    opt_debug = opt_verbose = 1;
//                    break;
//
//                case 'D':                                                                    // -D, --delete
//                    opt_delete = 1;
//                    break;
//
//                case 'f':                                                                    // -f, --force
//                    opt_overwrite = 1;
//                    break;
//
//                case 'k':                                                                    // -k, --keep
//                    opt_keep = 1;
//                    break;
//
//                case 'l':                                                                    // -l, --list
//                    opt_list = 1;
//                    break;
//
//                case 'o':                                                                    // -o, --output=DIR
//                {
//                    struct stat
//                    st;
//                    if (stat(optarg, & st))
//                    ErrERR("%s", optarg);
//                    if (!S_ISDIR(st.st_mode))
//                        Err("%s: %s", optarg, _("not a directory"));
//                    opt_output_dir = optarg;
//                    while (opt_output_dir[strlen(opt_output_dir) - 1] == '/')
//                        opt_output_dir[strlen(opt_output_dir) - 1] = '\0';
//                }
//                break;
//
//                case 'p':                                                                    // -p, --paths
//                    opt_keep_paths = 1;
//                    break;
//
//                case 'q':                                                                    // -q, --quiet
//                    opt_debug = opt_verbose = 0;
//                    break;
//
//                case 'R':
//                case 'r':                                                                    // -r, --recursive
//                    opt_scan = opt_recursive = 1;
//                    break;
//
//                case 's':                                                                    // -s, --scan
//                    opt_scan = 1;
//                    break;
//
//                case 't':                                                                    // -t, --test
//                    opt_test = 1;
//                    break;
//
//                default:
//                    usage(EXIT_FAILURE);
//            }
//        }
//
//        /* Set these options for the routines in "error.c" in the library */
//        err_debug = opt_debug;
//        err_verbose = opt_verbose;
//
//        while (optind < argc)
//            queue_file(argv[optind++], & file_errs);
//        if (file_errs)
//            exit(EXIT_FAILURE);
//
//        /* If nothing was specified, scan by default */
//        if (!opt_scan && !opt_recursive && !num_yfiles)
//            opt_scan = 1;
//    }
///*--- cmdline() ---------------------------------------------------------------------------------*/
//
//
//    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//        SCAN_FILES
//        Scans the current directory, the output directory, and any directories recursively.
//    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
//    static void
//    scan_files(const char*path) {
//        DIR * dirf;
//        struct dirent*dn;
//        struct stat
//        st;
//        char filename[
//        PATH_MAX];
//
//        if (!(dirf = opendir(path ? path : ".")))
//            return;
//        while ((dn = readdir(dirf))) {
//            if (dn - > d_name[0] == '.' && dn - > d_name[1] == '\0')
//                continue;
//            if (dn - > d_name[0] == '.' && dn - > d_name[1] == '.' && dn - > d_name[2] == '\0')
//                continue;
//            if (!path)
//                strncpy(filename, dn - > d_name, sizeof(filename) - 1);
//            else
//                snprintf(filename, sizeof(filename), "%s/%s", path, dn - > d_name);
//            if (stat(filename, & st))
//            continue;
//            if (S_ISDIR(st.st_mode) && opt_recursive)
//                scan_files(filename);
//            else if (!S_ISDIR(st.st_mode)) {
//                YDECFILE * y = ydecfile_create(filename, opt_strict);
//                if (y) {
//                    yfiles = (YDECFILE * *)xrealloc(yfiles, (num_yfiles + 1) * sizeof(YDECFILE *));
//                    yfiles[num_yfiles++] = y;
//                }
//            }
//        }
//        closedir(dirf);
//    }
///*--- scan_files() ------------------------------------------------------------------------------*/
//
//
//    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//        SCAN_FOR_FILES
//        Scans directories if necessary.
//    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
//    static void
//    scan_for_files(void) {
//        /* Do general recursive scan if no dirs were specified on the command line */
//        if (!num_dirs)
//            scan_files(NULL);
//        else                                                            /* Scan specified dirs */ {
//            register
//            int ct;
//
//            for (ct = 0; ct < num_dirs; ct++)
//                scan_files(dirs[ct]);
//        }
//    }
///*--- scan_for_files() --------------------------------------------------------------------------*/
//
//
//    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//        FLAG_MULTIPART_FILE
//        Attempts to determine which of the input files (if any) are multipart archives.
//    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
//    static void
//    flag_multipart_file(YDECFILE*y) {
//        register
//        int ct;
//
//        /* If the part number is 0 (i.e. part not present or '0' explicitly specified) consider
//          it a single part archive always */
//        if (y - > header - > part == 0)
//            return;
//
//        /* If we found another file with the same name, it's a multipart */
//        for (ct = 0; ct < num_yfiles; ct++) {
//            if (!strcmp(y - > header - > name, yfiles[ct] - > header - > name)) {
//                y - > multipart = 1;
//                return;
//            }
//        }
//    }
///*--- flag_multipart_file() ---------------------------------------------------------------------*/
//
//
//    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//        SET_OUTPUT_FILENAME
//        Sets output_filename for each item in the list of files.
//    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
//    static void
//    set_output_filename(YDECFILE*y) {
//        char buf[
//        PATH_MAX];                                            /* Copy of input filename for mangling */
//        char*path,*file;                                            /* Path and file components */
//        char outfile[
//        PATH_MAX];                                    /* Output filename buffer */
//
//        strncpy(buf, y - > header - > name, sizeof(buf) - 1);
//        outfile[0] = '\0';
//
//        /* Start with opt_output_dir if present */
//        if (opt_output_dir) {
//            strncat(outfile, opt_output_dir, sizeof(outfile) - strlen(outfile) - 1);
//            if (outfile[strlen(outfile) - 1] != '/')
//                strncat(outfile, "/", sizeof(outfile) - strlen(outfile) - 1);
//        }
//
//        /* Filename with path */
//        if ((file = strrchr(buf, '/'))) {
//            *file++ = '\0';
//            path = buf;
//
//            if (opt_output_dir &&*path == '/')                    /* Make absolute path relative */
//            path++;
//
//            if (opt_keep_paths) {
//                strncat(outfile, path, sizeof(outfile) - strlen(outfile) - 1);
//                if (outfile[strlen(outfile) - 1] != '/')
//                    strncat(outfile, "/", sizeof(outfile) - strlen(outfile) - 1);
//            }
//        }
//        /* Filename without a path */
//        else {
//            path = NULL;
//            file = buf;
//        }
//
//        strncat(outfile, file, sizeof(outfile) - strlen(outfile) - 1);
//        y - > output_filename = xstrdup(outfile);
//    }
///*--- set_output_filename() ---------------------------------------------------------------------*/
//
//
///* Macro to "output" a byte using the output buffer */
//    #
//
//    define OUT(c)
//
//    \
//    outbuf[ob]=(unsigned char)c;\
//            if(++ob==BUFSIZ)                                                                         \
//
//    {\
//        if (out && (fwrite(outbuf, sizeof(unsignedchar),ob, out)!=ob))\
//        ErrERR("%s", y - > output_filename);\
//        ob = 0;\
//    }
//
//    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//        YDECODE_DATA
//        Reads data from `in', decodes the data, and outputs to `out'.
//        Returns 0 on success, -1 on error.
//        Optionally sets pcrc, crc, encsize (encoded size), and decsize (decoded size).
//    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
//    static int
//    ydecode_data(YDECFILE*y, FILE*in, FILE*out, crc32_t*crc32p, crc32_t*pcrc32p,
//                 size_t*elen, size_t*dlen) {
//        unsigned
//        char inbuf[
//        BUFSIZ],outbuf[BUFSIZ];            /* Input buffer */
//        register unsigned char*b;                                    /* Current location in `buf' */
//        register
//        int ob;                                                /* Current offset in `outbuf' */
//        register
//        int linect;
//        register unsigned
//        long lineno;
//        register
//        int last_line_length, char_escaped;
//
//        ob = linect = last_line_length = char_escaped = 0;
//        lineno = 0;
//
//        while (fgets(inbuf, sizeof(inbuf), in)) {
//            if (YKEYWORD_END(inbuf))
//                break;
//
//            /* If strict checking is enabled, warn if we found a line of invalid length */
//            if (opt_strict && last_line_length && y - > header && (last_line_length != * y - > header - > line))
//            if (!char_escaped || (char_escaped && (last_line_length - 1 != * y - > header - > line)))
//            Notice(_("%s:%lu: invalid line length %d (should be %lu)"),
//                    y - > input_filename, lineno + y - > line_offset, last_line_length, * y - > header - > line);
//
//            lineno++;
//
//            for (b = inbuf;*b;
//            b++)
//            {
//                /* If strict checking is enabled, warn about invalid first and/or last characters in line */
//                if (opt_strict) {
//                    if (linect == 0) {
//                        if (*b == ' ')
//                        Verbose("%s:%lu:%d: %s", y - > input_filename, lineno + y - > line_offset, linect + 1,
//                                _("line begins with an unescaped space"));
//                        else if (*b == '\t')
//                        Verbose("%s:%lu:%d: %s", y - > input_filename, lineno + y - > line_offset, linect + 1,
//                                _("line begins with an unescaped TAB"));
//                        else if (*b == '.' &&*(b + 1) != '.')
//                        Verbose("%s:%lu:%d: %s", y - > input_filename, lineno + y - > line_offset, linect + 1,
//                                _("line begins with an unescaped single dot"));
//                    }
//                    if (*(b + 1) == '\r' ||*(b + 1) == '\n')
//                    {
//                        if (*b == ' ')
//                        Verbose("%s:%lu:%d: %s", y - > input_filename, lineno + y - > line_offset, linect + 1,
//                                _("line ends with an unescaped space"));
//                        else if (*b == '\t')
//                        Verbose("%s:%lu:%d: %s", y - > input_filename, lineno + y - > line_offset, linect + 1,
//                                _("line ends with an unescaped TAB"));
//                    }
//                }
//
//                /* If this is the beginning of a line, and it's a plain dot, and the next character is also a dot,
//                    move one character forward. */
//                if (linect == 0 &&*b == '.' &&*(b + 1) == '.')
//                b++;
//
//                /* For all relevant characters, set char_escaped if it was escaped (for strict checking line length) */
//                if (*b != '\n' &&*b != '\r')
//                char_escaped = ( * b == '=');
//
//                switch (*b)
//                {
//                    case '\r':
//                        continue;
//
//                    case '\n':
//                        last_line_length = linect;
//                        linect = 0;
//                        continue;
//
//                    case '=':
//                        if (elen)*elen += 1;
//                        b++;
//                        linect++;
//                        *b = YUNESCAPE( * b);
//                        if (opt_strict && !YESCAPE_MAKES_SENSE( * b))
//                        Verbose("%s:%lu:%d: %s: %02X", y - > input_filename, lineno + y - > line_offset, linect + 1,
//                                _("byte escaped for no good reason"), * b);
//                        /* FALLTHROUGH */
//
//                    default:
//                        *b = YDECODE( * b);
//                        if (elen)*elen += 1;
//                        if (dlen)*dlen += 1;
//                        if (crc32p) CRC_UPDATE( * crc32p,*b);
//                        if (pcrc32p) CRC_UPDATE( * pcrc32p,*b);
//                        OUT( * b);
//                        linect++;
//                        break;
//                }
//            }
//        }
//        if (ob && out && (fwrite(outbuf, sizeof(unsignedchar),ob, out)!=ob))
//        ErrERR("%s", y - > output_filename);
//        if (ferror(in))
//            ErrERR("%s", y - > input_filename);
//        if (out && ferror(out))
//            ErrERR("%s", y - > output_filename);
//
//        return (0);
//    }
///*--- ydecode_data() ----------------------------------------------------------------------------*/
//
//
//    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//        YDECODE_SINGLE
//        Decodes the specified single part file.
//    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
//    static int
//    ydecode_single(YDECFILE*y) {
//        FILE * in,*out;                                            /* Input and output file pointers */
//        crc32_t crc32;                                                /* CRC value */
//        int errors;                                                /* Number of errors found */
//        char errmsg[
//        PATH_MAX];                                    /* Error message to insert when renaming */
//        size_t encodedsize, decodedsize;                        /* Length of encoded and decoded data */
//
//        encodedsize = decodedsize = (size_t) 0;
//        errors = 0;
//        errmsg[0] = '\0';
//
//        if (!(in = fopen(y - > input_filename, "r")))
//            ErrERR("%s", y - > input_filename);
//        if (fseek(in, y - > data_start, SEEK_SET))                /* Move input fp to data start */
//            ErrERR("%s", y - > input_filename);
//
//        if (opt_test)
//            out = NULL;
//        else if (!(out = open_output_file(y - > output_filename, opt_overwrite, y - > input_filename)))
//            return (-1);
//
//        /* Decode the data */
//        CRC_START(crc32);
//        ydecode_data(y, in, out, & crc32, NULL,&encodedsize,&decodedsize);
//        CRC_FINISH(crc32);
//
//        /* Clean up */
//        fclose(in);
//        if (out)
//            fclose(out);
//
//        /* Check for errors */
//        if (y - > header - > size && ( * y - > header - > size != decodedsize))
//        {
//            usermsg(YNAMES(y), 1, 1, _("file size mismatch"),
//                    "(%s=%s %s=%s)",
//                    _("ybegin"), comma1( * y - > header - > size),
//                    _("actual"), comma2(decodedsize));
//            errors++;
//            snprintf(errmsg, sizeof(errmsg), "size-%u", decodedsize);
//        }
//        if (y - > footer - > crc32 && ( * y - > footer - > crc32 != crc32))
//        {
//            usermsg(YNAMES(y), 1, 1, _("CRC mismatch"),
//                    "(%s=%08x %s=%08x)",
//                    _("yend"), * y - > footer - > crc32,
//                    _("actual"), crc32);
//            errors++;
//            snprintf(errmsg, sizeof(errmsg), "crc-%08x", crc32);
//        }
//        else if (opt_debug && y - > footer - > crc32 && ( * y - > footer - > crc32 == crc32))
//        usermsg(YNAMES(y), 1, 1, _("CRC OK"),
//                "(%s=%08x %s=%08x)",
//                _("yend"), * y - > footer - > crc32,
//                _("actual"), crc32);
//        else if (opt_debug && !y - > footer - > crc32)
//            usermsg(YNAMES(y), 1, 1, _("no CRC for file"), NULL);
//
//        if (errors && !opt_keep && !opt_test) {
//            if (!unlink(y - > output_filename))
//                usermsg(y - > output_filename, NULL, 0, 0, _("errors found, output file removed"), NULL);
//            else
//                usermsg(y - > output_filename, NULL, 0, 0, _("unable to remove output file"), strerror(errno));
//        } else if (!errors && opt_verbose) {
//            if (opt_delete && !opt_test) {
//                if (unlink(y - > input_filename))
//                    usermsg(y - > output_filename, NULL, 1, 1, _("file OK"), "(%.2f%%) (%s: %s)",
//                            PCT(decodedsize, encodedsize), _("input file not deleted"), strerror(errno));
//                else
//                    usermsg(y - > output_filename, NULL, 1, 1, _("file OK"), "(%.2f%%) (%s)",
//                            PCT(decodedsize, encodedsize), _("input file deleted"));
//            } else
//                usermsg(y - > output_filename, NULL, 1, 1, _("file OK"), "(%.2f%%)",
//                        PCT(decodedsize, encodedsize));
//        } else if (errors &&*errmsg && !opt_test)
//        {
//            char*newname = rename_output_file(y - > output_filename, errmsg);
//
//            if (newname)
//                usermsg(y - > output_filename, NULL, 0, 0, NULL, _("file renamed to `%s'"), newname);
//            else
//                usermsg(y - > output_filename, NULL, 0, 0, _("unable to rename output file"), strerror(errno));
//        }
//
//        return (0);
//    }
///*--- ydecode_single() --------------------------------------------------------------------------*/
//
//
//    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//        VERIFY_MULTI_FILE
//        Verifies a single file of a multipart archive.
//    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
//    static void
//    verify_multi_file(int part, int total, int first, int last, unsigned long*begin, int*errors) {
//        YDECFILE * y = NULL;                                                /* File currently being processed */
//        int ct;                                                        /* Current position in yfiles */
//
//        /* Find the yfile containing this part number */
//        for (ct = first; ct < last && !y; ct++)
//            if (yfiles[ct] - > header - > part && ( * yfiles[ct] - > header - > part == part))
//        y = yfiles[ct];
//
//        /* Part number not found - try to find it based on begin offset */
//        if (!y) {
//            for (ct = first; ct < last && !y; ct++)
//                if (yfiles[ct] - > part && yfiles[ct] - > part - > begin && ( * yfiles[ct] - > part - > begin ==*begin))
//            {
//                usermsg(YNAMES(yfiles[ct]), 0, 0, NULL,
//                        _("file designated as part %d really begins where part %d should"),
//                        * yfiles[ct] - > header - > part, part);
//                y = yfiles[ct];
//                *errors += 1;
//            }
//        }
//
//        /* Part number STILL not found - part is missing */
//        if (!y) {
//            usermsg(yfiles[first] - > output_filename, NULL, part, total, _("part missing"), NULL);
//            *errors += 1;
//            return;
//        }
//
//        /* Make sure a `=ypart' was present */
//        if (!y - > part) {
//            usermsg(YNAMES(y), part, total, _("missing `=ypart' header"), NULL);
//            *errors += 1;
//            return;
//        }
//
//        /* Make sure this part begins where we think it should */
//        if (y - > part - > begin && ( * y - > part - > begin !=*begin))
//        {
//            usermsg(YNAMES(y), part, total, _("incorrect beginning offset"),
//                    "(%s: %s %s: %s)",
//                    _("want"), comma1( * begin),
//                    _("got"), comma2( * y - > part - > begin));
//            *errors += 1;
//        }
//
//        /* Make sure size of part was specified */
//        if (!y - > footer - > size) {
//            usermsg(YNAMES(y), part, total, _("size of part not specified in `=yend'"), NULL);
//            *errors += 1;
//            return;
//        }
//
//        /* Advance begin pointer for next file */
//        *begin +=*y - > footer - > size;
//
//        return;
//    }
///*--- verify_multi_file() -----------------------------------------------------------------------*/
//
//
//    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//        VERIFY_MULTI
//        Examines all parts of the current archive.  Attempts to determine the total number of parts,
//        and if all parts of this archive are present.
//        Returns 0 if everything looks OK, or the number of errors found.
//    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
//    static int
//    verify_multi(int first, int last, int*total) {
//        int ct;                                                        /* Current position in yfiles */
//        int errors = 0;                                                /* Number of errors found */
//        int part = 0, highpart = 0;                                /* Counters for calculating total parts */
//        unsigned
//        long next_begin;                                                /* Next expected `begin' value */
//
//        /* Look for a total part number specified in any part of the archive.  Save the highest part
//          number found and use it if no other total is known */
//        for (*total = 0, ct = first;
//        ct<last
//        ;
//        ct++)
//        {
//            if (yfiles[ct] - > header - > total)
//            *total =*yfiles[ct] - > header - > total;
//            if (yfiles[ct] - > header - > part && ( * yfiles[ct] - > header - > part > highpart))
//            highpart =*yfiles[ct] - > header - > part;
//        }
//        if (*total)
//        Debug(_("%s: %d parts total"), yfiles[first] - > output_filename, * total);
//        else
//        {
//            *total = highpart;
//            Debug(_("%s: %d parts total (estimated)"), yfiles[first] - > output_filename, * total);
//        }
//
//        /* Now examine the file list one at a time, making sure each part is present and with the
//          correct `begin' offset */
//        for (next_begin = 1, part = 1; part <=*total;
//        part++)
//        {
//            Debug(_("%s: verifying part %d of %d"), yfiles[first] - > output_filename, part, * total);
//            verify_multi_file(part, * total, first, last,&next_begin,&errors);
//        }
//
//        return (errors);
//    }
///*--- verify_multi() ----------------------------------------------------------------------------*/
//
//
//    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//        YDECODE_MULTI
//        Decodes the specified multipart file beginning at the record specified by `pos' in yfiles.
//    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
//    static int
//    ydecode_multi(int*first_pos) {
//        int first, last;                                        /* Offset of first and last file in group */
//        YDECFILE * y = yfiles[ * first_pos];                        /* File currently being processed */
//        FILE * in,*out = NULL;                                    /* Input and output file pointers */
//        int errors = 0;                                            /* Number of errors found */
//        int ct;                                                    /* Current position in yfiles */
//        crc32_t pcrc32, crc32;                                        /* CRC of current part and total file */
//        size_t encodedsize = 0, decodedsize = 0;            /* Length of encoded and decoded data */
//        size_t encpart, decpart;                                    /* Length of data, this file */
//        int verify_errors;                                        /* Number of errors found in verification */
//        int total;                                                /* Total number of parts */
//        char errmsg[
//        PATH_MAX];                                    /* Error message to insert when renaming */
//
//        errmsg[0] = '\0';
//
//        /* Get offset in `yfiles' for last file in this file group */
//        first =*first_pos;
//        for (last = first;
//             last < num_yfiles && !strcmp(yfiles[last] - > header - > name, yfiles[first] - > header - > name);
//             last++)
//            /* DONOTHING */
//            ;
//        *first_pos = last - 1;
//
//        /* Verify consistency of header data for all parts before decoding to save time */
//        if ((verify_errors = verify_multi(first, last, & total))&&!opt_keep)
//        {
//            usermsg(yfiles[first] - > output_filename, NULL, 0, 0, _("errors found, nothing done"), NULL);
//            return (-1);
//        }
//
//        if (opt_test)
//            out = NULL;
//        else if (!(out = open_output_file(yfiles[first] - > output_filename, opt_overwrite,
//                yfiles[first] - > input_filename)))
//            return (-1);
//
//        /* Process each file in part */
//        CRC_START(crc32);
//        for (ct = first; ct < last; ct++) {
//            y = yfiles[ct];
//
//            if (!(in = fopen(y - > input_filename, "r")))
//                ErrERR("%s", y - > input_filename);
//            if (fseek(in, y - > data_start, SEEK_SET))            /* Move input fp to data start */
//                ErrERR("%s", y - > input_filename);
//            encpart = decpart = 0;
//
//            /*
//           **  Decode the file
//           */
//            CRC_START(pcrc32);
//            ydecode_data(y, in, out, & crc32,&pcrc32,&encpart,&decpart);
//            CRC_FINISH(pcrc32);
//            encodedsize += encpart;
//            decodedsize += decpart;
//            fclose(in);
//
//            if (y - > footer - > size && ( * y - > footer - > size != decpart))
//            {
//                usermsg(YNAMES(y), * y - > header - > part, total, _("file size mismatch"),
//                        "(%s=%s %s=%s)",
//                        _("yend"), comma1( * y - > footer - > size),
//                _("actual"), comma2(decpart));
//                snprintf(errmsg, sizeof(errmsg), "psize-%u", decpart);
//                errors++;
//            }
//            if (y - > footer - > pcrc32 && ( * y - > footer - > pcrc32 != pcrc32))
//            {
//                usermsg(YNAMES(y), * y - > header - > part, total, _("CRC mismatch"),
//                        "(%s=%08x %s=%08x)",
//                        _("yend"),*y - > footer - > pcrc32,
//                    _("actual"), pcrc32);
//                snprintf(errmsg, sizeof(errmsg), "pcrc-%08x", pcrc32);
//                errors++;
//            }
//            else if (opt_debug && y - > footer - > pcrc32 && ( * y - > footer - > pcrc32 == pcrc32))
//            usermsg(YNAMES(y), * y - > header - > part, total, _("CRC OK"),
//                    "(%s=%08x %s=%08x)",
//                    _("yend"),*y - > footer - > pcrc32,
//                    _("actual"), pcrc32);
//            else if (opt_debug && !y - > footer - > pcrc32)
//                usermsg(YNAMES(y), * y - > header - > part, total, _("no CRC for part"), NULL);
//            else if (opt_debug)
//                usermsg(YNAMES(y), * y - > header - > part, total, _("file OK"), "(%.2f%%)", PCT(decpart, encpart));
//        }
//        CRC_FINISH(crc32);
//
//        if (out)
//            fclose(out);
//
//        if (y - > header - > size && ( * y - > header - > size != decodedsize))
//        {
//            usermsg(y - > output_filename, NULL, 0, 0, _("file size mismatch"),
//                    "(%s=%s %s=%s)",
//                    _("ybegin"), comma1( * y - > header - > size),
//                    _("actual"), comma2(decodedsize));
//            snprintf(errmsg, sizeof(errmsg), "size-%u", decodedsize);
//            errors++;
//        }
//        if (y - > footer - > crc32 && ( * y - > footer - > crc32 != crc32))
//        {
//            usermsg(y - > output_filename, NULL, 0, 0, _("CRC mismatch"),
//                    "(%s=%08x %s=%08x)",
//                    _("yend"), * y - > footer - > crc32,
//                    _("actual"), crc32);
//            snprintf(errmsg, sizeof(errmsg), "crc-%08x", crc32);
//            errors++;
//        }
//        else if (opt_debug && y - > footer - > crc32 && ( * y - > footer - > crc32 == crc32))
//        usermsg(y - > output_filename, NULL, 0, 0, _("CRC OK"),
//                "(%s=%08x %s=%08x)",
//                _("yend"), * y - > footer - > crc32,
//                _("actual"), crc32);
//        else if (opt_debug && !y - > footer - > crc32)
//            usermsg(y - > output_filename, NULL, 0, 0, _("no CRC for file"), NULL);
//
//        /* Delete output file if errors occurred, output summary */
//        if (errors && !opt_keep && !opt_test) {
//            if (!unlink(y - > output_filename))
//                usermsg(y - > output_filename, NULL, 0, 0, _("errors found, output file removed"), NULL);
//            else
//                usermsg(y - > output_filename, NULL, 0, 0, _("unable to remove output file"), strerror(errno));
//        } else if (errors &&*errmsg && !opt_test)
//        {
//            char*newname = rename_output_file(y - > output_filename, errmsg);
//
//            if (newname)
//                usermsg(y - > output_filename, NULL, 0, 0, NULL, _("file renamed to `%s'"), newname);
//            else
//                usermsg(y - > output_filename, NULL, 0, 0, _("unable to rename output file"), strerror(errno));
//        }
//        if (errors)
//            return (-1);
//
//        /* Anything below here is working with a successfully decoded file */
//        if (opt_verbose) {
//            if (verify_errors)
//                usermsg(y - > output_filename, NULL, 0, 0, _("errors detected in multipart archive"), NULL);
//            else {
//                char msgbuf[
//                80];
//
//                if (total == 1)
//                    snprintf(msgbuf, sizeof(msgbuf), _("%d part OK, file OK"), total);
//                else
//                    snprintf(msgbuf, sizeof(msgbuf), _("%d parts OK, file OK"), total);
//
//                /* Remember to delete all files in the set */
//                if (opt_delete && !opt_test) {
//                    for (ct = first; ct < last; ct++) {
//                        y = yfiles[ct];
//                        if (unlink(y - > input_filename))
//                            WarnERR("%s: error deleting file", y - > input_filename);
//                    }
//                    usermsg(y - > output_filename, NULL, 0, 0, msgbuf, "(%.2f%%) (%s)",
//                            PCT(decodedsize, encodedsize),
//                            (total == 1) ? _("input file deleted") : _("input files deleted"));
//                } else
//                    usermsg(y - > output_filename, NULL, 0, 0, msgbuf, "(%.2f%%)", PCT(decodedsize, encodedsize));
//            }
//        }
//
//        return (0);
//    }
///*--- ydecode_multi() ---------------------------------------------------------------------------*/
//
//
//    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//        OUTPUT_FILE_LIST
//        Outputs a list of files found with information about each.
//    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
//    static void
//    output_file_list(void) {
//        FILE * out;
//        register
//        int ct;
//
//        if (opt_list)
//            out = stdout;
//        else
//            out = stderr;
//
//        for (ct = 0; ct < num_yfiles; ct++) {
//            YDECFILE * y = yfiles[ct];
//
//            fprintf(out, "%10s: \"%s\"\n", _("input"), y - > input_filename);
//            fprintf(out, "%10s: \"%s\"\n", _("output"), y - > output_filename);
//            fprintf(out, "%10s: \"%s\"\n", _("filename"), y - > header - > name);
//            fprintf(out, "%10s: %s\n", _("size"), y - > header - > size ? comma( * y - > header - > size):
//            _("UNSPECIFIED"));
//
//            if (y - > part) {
//                fprintf(out, "%10s: %s\n", _("begin"), y - > part - > begin ? comma( * y - > part - > begin):
//                _("UNSPECIFIED"));
//                fprintf(out, "%10s: %s\n", _("end"), y - > part - > end ? comma( * y - > part - > end):_("UNSPECIFIED"))
//                ;
//            }
//            fprintf(out, "%10s: %s\n", _("psize"), y - > footer - > size ? comma( * y - > footer - > size):
//            _("UNSPECIFIED"));
//            if (y - > footer - > pcrc32)
//                fprintf(out, "%10s: %08x\n", _("pcrc32"), * y - > footer - > pcrc32);
//            else
//            fprintf(out, "%10s: %s\n", _("pcrc32"), _("UNSPECIFIED"));
//            if (y - > footer - > crc32)
//                fprintf(out, "%10s: %08x\n", _("crc32"), * y - > footer - > crc32);
//            else
//            fprintf(out, "%10s: %s\n", _("crc32"), _("UNSPECIFIED"));
//
//            if (y - > multipart) {
//                if (y - > header - > part && y - > header - > total)
//                    fprintf(out, _("%10s  multipart archive, part %d of %d\n"), " ", * y - > header - > part,*
//                y - > header - > total);
//                else if (y - > header - > part)
//                    fprintf(out, _("%10s  multipart archive, part %d\n"), " ", * y - > header - > part);
//            } else
//                fprintf(out, _("%10s  single part archive\n"), " ");
//
//            fprintf(out, "\n");
//        }
//        if (opt_list)
//            exit(EXIT_SUCCESS);
//    }
///*--- output_file_list() ------------------------------------------------------------------------*/
//
//
//    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//        MAIN
//    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
//    int
//    main(int argc, char**argv) {
//        int ct;
//
//        set_progname(argv[0]);
//        setlocale(LC_ALL, "");
//        bindtextdomain(PACKAGE, LOCALEDIR);
//        textdomain(PACKAGE);
//        cmdline(argc, argv);
//
//        /* Scan for files if requested */
//        if (opt_scan || opt_recursive)
//            scan_for_files();
//
//        /* If there are no input files at this point, there's nothing else we can do */
//        if (!num_yfiles) {
//            Warn(_("no input files found"));
//            usage(EXIT_FAILURE);
//        }
//
//        /* Process the file list */
//        for (ct = 0; ct < num_yfiles; ct++) {
//            flag_multipart_file(yfiles[ct]);
//            set_output_filename(yfiles[ct]);
//        }
//        qsort(yfiles, num_yfiles, sizeof(YDECFILE *), ydecfile_cmp);
//
//        /* Output file list for --list option (or debug) */
//        if (opt_list || err_debug)
//            output_file_list();
//
//        /* Decode all files in the list */
//        for (ct = 0; ct < num_yfiles; ct++) {
//            if (yfiles[ct] - > multipart)
//                ydecode_multi( & ct);
//            else
//            ydecode_single(yfiles[ct]);
//        }
//
//        return (EXIT_SUCCESS);
//    }
///*--- main() ------------------------------------------------------------------------------------*/
//
///* vi:set ts=3: */
