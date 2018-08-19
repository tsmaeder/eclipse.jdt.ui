/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     istvan@benedek-home.de - 103706 [formatter] indent empty lines
 *     Aaron Luchko, aluchko@redhat.com - 105926 [Formatter] Exporting Unnamed profile fails silently
 *     Brock Janiczak <brockj@tpg.com.au> - [formatter] Add  option: "add new line after label" - https://bugs.eclipse.org/bugs/show_bug.cgi?id=150741
 *     Ray V. (voidstar@gmail.com) - Contribution for bug 282988
 *     Harry Terkelsen (het@google.com) - Bug 449262 - Allow the use of third-party Java formatters
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class FormatterMessages extends NLS {

	private static final String BUNDLE_NAME= FormatterMessages.class.getName();

	private FormatterMessages() {
		// Do not instantiate
	}

	public static String FormatterModifyDialog_blankLines_pref_after_import;
	public static String FormatterModifyDialog_blankLines_pref_after_package;
	public static String FormatterModifyDialog_blankLines_pref_at_beginning_of_method_body;
	public static String FormatterModifyDialog_blankLines_pref_before_decls_of_same_kind;
	public static String FormatterModifyDialog_blankLines_pref_before_field_decls;
	public static String FormatterModifyDialog_blankLines_pref_before_first_decl;
	public static String FormatterModifyDialog_blankLines_pref_before_import;
	public static String FormatterModifyDialog_blankLines_pref_before_member_class_decls;
	public static String FormatterModifyDialog_blankLines_pref_before_method_decls;
	public static String FormatterModifyDialog_blankLines_pref_before_package;
	public static String FormatterModifyDialog_blankLines_pref_between_import_groups;
	public static String FormatterModifyDialog_blankLines_pref_between_type_declarations;
	public static String FormatterModifyDialog_blankLines_pref_empty_lines_to_preserve;
	public static String FormatterModifyDialog_blankLines_tree_blank_lines;
	public static String FormatterModifyDialog_blankLines_tree_class_declarations;
	public static String FormatterModifyDialog_blankLines_tree_compilation_unit;
	public static String FormatterModifyDialog_braces_pref_annotation_type_declaration;
	public static String FormatterModifyDialog_braces_pref_anonymous_class_declaration;
	public static String FormatterModifyDialog_braces_pref_array_initializer;
	public static String FormatterModifyDialog_braces_pref_blocks;
	public static String FormatterModifyDialog_braces_pref_blocks_in_case;
	public static String FormatterModifyDialog_braces_pref_class_declaration;
	public static String FormatterModifyDialog_braces_pref_constructor_declaration;
	public static String FormatterModifyDialog_braces_pref_enum_declaration;
	public static String FormatterModifyDialog_braces_pref_enumconst_declaration;
	public static String FormatterModifyDialog_braces_pref_keep_empty_array_initializer_on_one_line;
	public static String FormatterModifyDialog_braces_pref_lambda_body;
	public static String FormatterModifyDialog_braces_pref_method_declaration;
	public static String FormatterModifyDialog_braces_pref_switch_case;
	public static String FormatterModifyDialog_braces_tree_brace_positions;
	public static String FormatterModifyDialog_braces_val_next_line;
	public static String FormatterModifyDialog_braces_val_next_line_indented;
	public static String FormatterModifyDialog_braces_val_next_line_on_wrap;
	public static String FormatterModifyDialog_braces_val_same_line;
	public static String FormatterModifyDialog_comments_pref_blank_line_before_javadoc_tags;
	public static String FormatterModifyDialog_comments_pref_enable_block;
	public static String FormatterModifyDialog_comments_pref_enable_javadoc;
	public static String FormatterModifyDialog_comments_pref_enable_line;
	public static String FormatterModifyDialog_comments_pref_format_code_snippets;
	public static String FormatterModifyDialog_comments_pref_format_header;
	public static String FormatterModifyDialog_comments_pref_format_html;
	public static String FormatterModifyDialog_comments_pref_format_line_comments_on_first_column;
	public static String FormatterModifyDialog_comments_pref_indent_description_after_param;
	public static String FormatterModifyDialog_comments_pref_javadoc_align;
	public static String FormatterModifyDialog_comments_pref_javadoc_align_none;
	public static String FormatterModifyDialog_comments_pref_javadoc_align_names_and_descriptions;
	public static String FormatterModifyDialog_comments_pref_javadoc_align_descriptions_grouped;
	public static String FormatterModifyDialog_comments_pref_javadoc_align_descriptions_to_tag;
	public static String FormatterModifyDialog_comments_pref_line_width;
	public static String FormatterModifyDialog_comments_pref_line_width_count_from_starting_position;
	public static String FormatterModifyDialog_comments_pref_never_indent_block_comments_on_first_column;
	public static String FormatterModifyDialog_comments_pref_never_indent_line_comments_on_first_column;
	public static String FormatterModifyDialog_comments_pref_never_join_lines;
	public static String FormatterModifyDialog_comments_pref_new_line_after_param_tags;
	public static String FormatterModifyDialog_comments_pref_new_lines_at_comment_boundaries;
	public static String FormatterModifyDialog_comments_pref_new_lines_at_javadoc_boundaries;
	public static String FormatterModifyDialog_comments_pref_preserve_white_space_before_line_comment;
	public static String FormatterModifyDialog_comments_pref_remove_blank_lines;
	public static String FormatterModifyDialog_comments_tree_block_comments;
	public static String FormatterModifyDialog_comments_tree_comments;
	public static String FormatterModifyDialog_comments_tree_javadocs;
	public static String FormatterModifyDialog_indentation_info_blank_lines_to_preserve;
	public static String FormatterModifyDialog_indentation_pref_align_fields_in_columns;
	public static String FormatterModifyDialog_indentation_pref_align_with_spaces;
	public static String FormatterModifyDialog_indentation_pref_blank_lines_separating_independent_groups;
	public static String FormatterModifyDialog_indentation_pref_indent_break_statements;
	public static String FormatterModifyDialog_indentation_pref_indent_declarations_within_annot_decl;
	public static String FormatterModifyDialog_indentation_pref_indent_declarations_within_class_body;
	public static String FormatterModifyDialog_indentation_pref_indent_declarations_within_enum_const;
	public static String FormatterModifyDialog_indentation_pref_indent_declarations_within_enum_decl;
	public static String FormatterModifyDialog_indentation_pref_indent_empty_lines;
	public static String FormatterModifyDialog_indentation_pref_indent_size;
	public static String FormatterModifyDialog_indentation_pref_indent_statements_compare_to_block;
	public static String FormatterModifyDialog_indentation_pref_indent_statements_compare_to_body;
	public static String FormatterModifyDialog_indentation_pref_indent_statements_within_case_body;
	public static String FormatterModifyDialog_indentation_pref_indent_statements_within_switch_body;
	public static String FormatterModifyDialog_indentation_pref_tab_policy;
	public static String FormatterModifyDialog_indentation_pref_tab_size;
	public static String FormatterModifyDialog_indentation_pref_use_tabs_only_for_leading_indentations;
	public static String FormatterModifyDialog_indentation_tab_policy_MIXED;
	public static String FormatterModifyDialog_indentation_tab_policy_SPACE;
	public static String FormatterModifyDialog_indentation_tab_policy_TAB;
	public static String FormatterModifyDialog_indentation_tree_indentation;
	public static String FormatterModifyDialog_indentation_tree_indented_elements;
	public static String FormatterModifyDialog_lineWrap_indentation_policy_label;
	public static String FormatterModifyDialog_lineWrap_pref_annotations_arguments;
	public static String FormatterModifyDialog_lineWrap_pref_arguments;
	public static String FormatterModifyDialog_lineWrap_pref_array_init;
	public static String FormatterModifyDialog_lineWrap_pref_assignments;
	public static String FormatterModifyDialog_lineWrap_pref_binary_exprs;
	public static String FormatterModifyDialog_lineWrap_pref_catch;
	public static String FormatterModifyDialog_lineWrap_pref_compact_if_else;
	public static String FormatterModifyDialog_lineWrap_pref_compact_loops;
	public static String FormatterModifyDialog_lineWrap_pref_conditionals;
	public static String FormatterModifyDialog_lineWrap_pref_constant_arguments;
	public static String FormatterModifyDialog_lineWrap_pref_constants;
	public static String FormatterModifyDialog_lineWrap_pref_declaration;
	public static String FormatterModifyDialog_lineWrap_pref_default_indent_array;
	public static String FormatterModifyDialog_lineWrap_pref_default_indent_wrapped;
	public static String FormatterModifyDialog_lineWrap_pref_explicit_constructor_invocations;
	public static String FormatterModifyDialog_lineWrap_pref_extends_clause;
	public static String FormatterModifyDialog_lineWrap_pref_for;
	public static String FormatterModifyDialog_lineWrap_pref_implements_clause;
	public static String FormatterModifyDialog_lineWrap_pref_max_line_width;
	public static String FormatterModifyDialog_lineWrap_pref_module_statements;
	public static String FormatterModifyDialog_lineWrap_pref_never_join_lines;
	public static String FormatterModifyDialog_lineWrap_pref_object_allocation_arguments;
	public static String FormatterModifyDialog_lineWrap_pref_param_type_arguments;
	public static String FormatterModifyDialog_lineWrap_pref_param_type_parameters;
	public static String FormatterModifyDialog_lineWrap_pref_param_type_ref;
	public static String FormatterModifyDialog_lineWrap_pref_parameters;
	public static String FormatterModifyDialog_lineWrap_pref_qualified_invocations;
	public static String FormatterModifyDialog_lineWrap_pref_qualified_object_allocation_arguments;
	public static String FormatterModifyDialog_lineWrap_pref_superinterfaces;
	public static String FormatterModifyDialog_lineWrap_pref_throws_clause;
	public static String FormatterModifyDialog_lineWrap_pref_try;
	public static String FormatterModifyDialog_lineWrap_pref_wrap_outer_expressions_when_nested;
	public static String FormatterModifyDialog_lineWrap_tree_annotations;
	public static String FormatterModifyDialog_lineWrap_tree_class_decls;
	public static String FormatterModifyDialog_lineWrap_tree_constructor_decls;
	public static String FormatterModifyDialog_lineWrap_tree_enum_decls;
	public static String FormatterModifyDialog_lineWrap_tree_expressions;
	public static String FormatterModifyDialog_lineWrap_tree_function_calls;
	public static String FormatterModifyDialog_lineWrap_tree_line_wrapping;
	public static String FormatterModifyDialog_lineWrap_tree_method_decls;
	public static String FormatterModifyDialog_lineWrap_tree_module_descriptions;
	public static String FormatterModifyDialog_lineWrap_tree_parameterized_types;
	public static String FormatterModifyDialog_lineWrap_tree_statements;
	public static String FormatterModifyDialog_lineWrap_tree_wrapping_settings;
	public static String FormatterModifyDialog_lineWrap_val_always_wrap_first_others_when_necessary;
	public static String FormatterModifyDialog_lineWrap_val_do_not_split;
	public static String FormatterModifyDialog_lineWrap_val_force_split;
	public static String FormatterModifyDialog_lineWrap_val_indentation_by_one;
	public static String FormatterModifyDialog_lineWrap_val_indentation_default;
	public static String FormatterModifyDialog_lineWrap_val_indentation_on_column;
	public static String FormatterModifyDialog_lineWrap_val_wrap_after_operators;
	public static String FormatterModifyDialog_lineWrap_val_wrap_always;
	public static String FormatterModifyDialog_lineWrap_val_wrap_always_except_first_only_if_necessary;
	public static String FormatterModifyDialog_lineWrap_val_wrap_always_indent_all_but_first;
	public static String FormatterModifyDialog_lineWrap_val_wrap_before_operators;
	public static String FormatterModifyDialog_lineWrap_val_wrap_when_necessary;
	public static String FormatterModifyDialog_lineWrap_wrapping_policy_label;
	public static String FormatterModifyDialog_newLines_pref_after_labels;
	public static String FormatterModifyDialog_newLines_pref_after_opening_brace_of_array_initializer;
	public static String FormatterModifyDialog_newLines_pref_before_catch_statements;
	public static String FormatterModifyDialog_newLines_pref_before_closing_brace_of_array_initializer;
	public static String FormatterModifyDialog_newLines_pref_before_else_statements;
	public static String FormatterModifyDialog_newLines_pref_before_finally_statements;
	public static String FormatterModifyDialog_newLines_pref_before_while_in_do_statements;
	public static String FormatterModifyDialog_newLines_pref_empty_annotation_decl;
	public static String FormatterModifyDialog_newLines_pref_empty_anonymous_class_body;
	public static String FormatterModifyDialog_newLines_pref_empty_block;
	public static String FormatterModifyDialog_newLines_pref_empty_class_body;
	public static String FormatterModifyDialog_newLines_pref_empty_enum_constant;
	public static String FormatterModifyDialog_newLines_pref_empty_enum_declaration;
	public static String FormatterModifyDialog_newLines_pref_empty_method_body;
	public static String FormatterModifyDialog_newLines_pref_empty_statement;
	public static String FormatterModifyDialog_newLines_pref_end_of_file;
	public static String FormatterModifyDialog_newLines_pref_enum_constants;
	public static String FormatterModifyDialog_newLines_pref_fields;
	public static String FormatterModifyDialog_newLines_pref_keep_else_if_on_one_line;
	public static String FormatterModifyDialog_newLines_pref_keep_else_on_same_line;
	public static String FormatterModifyDialog_newLines_pref_keep_guardian_clause_on_one_line;
	public static String FormatterModifyDialog_newLines_pref_keep_simple_do_while_body_on_one_line;
	public static String FormatterModifyDialog_newLines_pref_keep_simple_for_body_on_one_line;
	public static String FormatterModifyDialog_newLines_pref_keep_simple_if_on_one_line;
	public static String FormatterModifyDialog_newLines_pref_keep_simple_while_body_on_one_line;
	public static String FormatterModifyDialog_newLines_pref_keep_then_on_same_line;
	public static String FormatterModifyDialog_newLines_pref_local_variables;
	public static String FormatterModifyDialog_newLines_pref_methods;
	public static String FormatterModifyDialog_newLines_pref_packages;
	public static String FormatterModifyDialog_newLines_pref_paramters;
	public static String FormatterModifyDialog_newLines_pref_type_annotations;
	public static String FormatterModifyDialog_newLines_pref_types;
	public static String FormatterModifyDialog_newLines_tree_after_annotations;
	public static String FormatterModifyDialog_newLines_tree_between_empty_braces;
	public static String FormatterModifyDialog_newLines_tree_control_statements;
	public static String FormatterModifyDialog_newLines_tree_if_else;
	public static String FormatterModifyDialog_newLines_tree_new_lines;
	public static String FormatterModifyDialog_newLines_tree_simple_loops;
	public static String FormatterModifyDialog_offOn_description;
	public static String FormatterModifyDialog_offOn_error_endsWithWhitespace;
	public static String FormatterModifyDialog_offOn_error_startsWithWhitespace;
	public static String FormatterModifyDialog_offOn_pref_enable;
	public static String FormatterModifyDialog_offOn_pref_off_tag;
	public static String FormatterModifyDialog_offOn_pref_on_tag;
	public static String FormatterModifyDialog_offOn_tree_off_on_tags;
	public static String FormatterModifyDialog_parentheses_pref_annotation;
	public static String FormatterModifyDialog_parentheses_pref_catch_clause;
	public static String FormatterModifyDialog_parentheses_pref_enum_constant_declaration;
	public static String FormatterModifyDialog_parentheses_pref_for_statement;
	public static String FormatterModifyDialog_parentheses_pref_if_while_statement;
	public static String FormatterModifyDialog_parentheses_pref_lambda_declaration;
	public static String FormatterModifyDialog_parentheses_pref_method_declaration;
	public static String FormatterModifyDialog_parentheses_pref_method_invocation;
	public static String FormatterModifyDialog_parentheses_pref_switch_statement;
	public static String FormatterModifyDialog_parentheses_pref_try_clause;
	public static String FormatterModifyDialog_parentheses_tree_parentheses_positions;
	public static String FormatterModifyDialog_parentheses_val_common_lines;
	public static String FormatterModifyDialog_parentheses_val_preserve_positions;
	public static String FormatterModifyDialog_parentheses_val_separate_lines_if_not_empty;
	public static String FormatterModifyDialog_parentheses_val_separate_lines;
	public static String FormatterModifyDialog_parentheses_val_separate_lines_if_wrapped;
	public static String FormatterModifyDialog_preview_custom_contents_toggle;
	public static String FormatterModifyDialog_preview_custom_contents_default_comment;
	public static String FormatterModifyDialog_preview_line_width_label;
	public static String FormatterModifyDialog_preview_show_whitespace_toggle;
	public static String FormatterModifyDialog_preview_show_raw_source_toggle;
	public static String FormatterModifyDialog_whiteSpace_pref_after_and_list;
	public static String FormatterModifyDialog_whiteSpace_pref_after_arrow_operator;
	public static String FormatterModifyDialog_whiteSpace_pref_after_assignment_operator;
	public static String FormatterModifyDialog_whiteSpace_pref_after_at;
	public static String FormatterModifyDialog_whiteSpace_pref_after_binary_operators;
	public static String FormatterModifyDialog_whiteSpace_pref_after_closing_angle_bracket;
	public static String FormatterModifyDialog_whiteSpace_pref_after_closing_brace;
	public static String FormatterModifyDialog_whiteSpace_pref_after_closing_paren;
	public static String FormatterModifyDialog_whiteSpace_pref_after_colon;
	public static String FormatterModifyDialog_whiteSpace_pref_after_comma;
	public static String FormatterModifyDialog_whiteSpace_pref_after_comma_const_arg;
	public static String FormatterModifyDialog_whiteSpace_pref_after_comma_decl;
	public static String FormatterModifyDialog_whiteSpace_pref_after_comma_fields;
	public static String FormatterModifyDialog_whiteSpace_pref_after_comma_implements;
	public static String FormatterModifyDialog_whiteSpace_pref_after_comma_in_alloc;
	public static String FormatterModifyDialog_whiteSpace_pref_after_comma_in_method_args;
	public static String FormatterModifyDialog_whiteSpace_pref_after_comma_in_params;
	public static String FormatterModifyDialog_whiteSpace_pref_after_comma_in_qalloc;
	public static String FormatterModifyDialog_whiteSpace_pref_after_comma_in_throws;
	public static String FormatterModifyDialog_whiteSpace_pref_after_comma_inc;
	public static String FormatterModifyDialog_whiteSpace_pref_after_comma_init;
	public static String FormatterModifyDialog_whiteSpace_pref_after_comma_localvars;
	public static String FormatterModifyDialog_whiteSpace_pref_after_ellipsis;
	public static String FormatterModifyDialog_whiteSpace_pref_after_opening_angle_bracket;
	public static String FormatterModifyDialog_whiteSpace_pref_after_opening_brace;
	public static String FormatterModifyDialog_whiteSpace_pref_after_opening_bracket;
	public static String FormatterModifyDialog_whiteSpace_pref_after_opening_paren;
	public static String FormatterModifyDialog_whiteSpace_pref_after_opening_paren_const_arg;
	public static String FormatterModifyDialog_whiteSpace_pref_after_postfix_operators;
	public static String FormatterModifyDialog_whiteSpace_pref_after_prefix_operators;
	public static String FormatterModifyDialog_whiteSpace_pref_after_question;
	public static String FormatterModifyDialog_whiteSpace_pref_after_semicolon;
	public static String FormatterModifyDialog_whiteSpace_pref_after_unary_operators;
	public static String FormatterModifyDialog_whiteSpace_pref_before_and_list;
	public static String FormatterModifyDialog_whiteSpace_pref_before_arrow_operator;
	public static String FormatterModifyDialog_whiteSpace_pref_before_assignment_operator;
	public static String FormatterModifyDialog_whiteSpace_pref_before_at;
	public static String FormatterModifyDialog_whiteSpace_pref_before_binary_operators;
	public static String FormatterModifyDialog_whiteSpace_pref_before_closing_angle_bracket;
	public static String FormatterModifyDialog_whiteSpace_pref_before_closing_brace;
	public static String FormatterModifyDialog_whiteSpace_pref_before_closing_bracket;
	public static String FormatterModifyDialog_whiteSpace_pref_before_closing_paren;
	public static String FormatterModifyDialog_whiteSpace_pref_before_closing_paren_const_arg;
	public static String FormatterModifyDialog_whiteSpace_pref_before_colon;
	public static String FormatterModifyDialog_whiteSpace_pref_before_colon_case;
	public static String FormatterModifyDialog_whiteSpace_pref_before_colon_default;
	public static String FormatterModifyDialog_whiteSpace_pref_before_comma;
	public static String FormatterModifyDialog_whiteSpace_pref_before_comma_const_arg;
	public static String FormatterModifyDialog_whiteSpace_pref_before_comma_decl;
	public static String FormatterModifyDialog_whiteSpace_pref_before_comma_fields;
	public static String FormatterModifyDialog_whiteSpace_pref_before_comma_implements;
	public static String FormatterModifyDialog_whiteSpace_pref_before_comma_in_alloc;
	public static String FormatterModifyDialog_whiteSpace_pref_before_comma_in_method_args;
	public static String FormatterModifyDialog_whiteSpace_pref_before_comma_in_params;
	public static String FormatterModifyDialog_whiteSpace_pref_before_comma_in_qalloc;
	public static String FormatterModifyDialog_whiteSpace_pref_before_comma_in_throws;
	public static String FormatterModifyDialog_whiteSpace_pref_before_comma_inc;
	public static String FormatterModifyDialog_whiteSpace_pref_before_comma_init;
	public static String FormatterModifyDialog_whiteSpace_pref_before_comma_localvars;
	public static String FormatterModifyDialog_whiteSpace_pref_before_ellipsis;
	public static String FormatterModifyDialog_whiteSpace_pref_before_opening_angle_bracket;
	public static String FormatterModifyDialog_whiteSpace_pref_before_opening_brace;
	public static String FormatterModifyDialog_whiteSpace_pref_before_opening_brace_decl;
	public static String FormatterModifyDialog_whiteSpace_pref_before_opening_brace_enum_const;
	public static String FormatterModifyDialog_whiteSpace_pref_before_opening_brace_of_a_class;
	public static String FormatterModifyDialog_whiteSpace_pref_before_opening_brace_of_anon_class;
	public static String FormatterModifyDialog_whiteSpace_pref_before_opening_bracket;
	public static String FormatterModifyDialog_whiteSpace_pref_before_opening_paren;
	public static String FormatterModifyDialog_whiteSpace_pref_before_opening_paren_annot_type;
	public static String FormatterModifyDialog_whiteSpace_pref_before_opening_paren_const_arg;
	public static String FormatterModifyDialog_whiteSpace_pref_before_parenthesized_expressions;
	public static String FormatterModifyDialog_whiteSpace_pref_before_postfix_operators;
	public static String FormatterModifyDialog_whiteSpace_pref_before_prefix_operators;
	public static String FormatterModifyDialog_whiteSpace_pref_before_question;
	public static String FormatterModifyDialog_whiteSpace_pref_before_semicolon;
	public static String FormatterModifyDialog_whiteSpace_pref_before_unary_operators;
	public static String FormatterModifyDialog_whiteSpace_pref_between_empty_braces;
	public static String FormatterModifyDialog_whiteSpace_pref_between_empty_brackets;
	public static String FormatterModifyDialog_whiteSpace_pref_between_empty_parens;
	public static String FormatterModifyDialog_whiteSpace_pref_between_empty_parens_annot_type;
	public static String FormatterModifyDialog_whiteSpace_pref_between_empty_parens_const_arg;
	public static String FormatterModifyDialog_whiteSpace_tree_annotation_types;
	public static String FormatterModifyDialog_whiteSpace_tree_annotations;
	public static String FormatterModifyDialog_whiteSpace_tree_arrayalloc;
	public static String FormatterModifyDialog_whiteSpace_tree_arraydecls;
	public static String FormatterModifyDialog_whiteSpace_tree_arrayelem;
	public static String FormatterModifyDialog_whiteSpace_tree_arrayinit;
	public static String FormatterModifyDialog_whiteSpace_tree_arrays;
	public static String FormatterModifyDialog_whiteSpace_tree_assert;
	public static String FormatterModifyDialog_whiteSpace_tree_assignments;
	public static String FormatterModifyDialog_whiteSpace_tree_blocks;
	public static String FormatterModifyDialog_whiteSpace_tree_calls;
	public static String FormatterModifyDialog_whiteSpace_tree_catch;
	public static String FormatterModifyDialog_whiteSpace_tree_classes;
	public static String FormatterModifyDialog_whiteSpace_tree_conditionals;
	public static String FormatterModifyDialog_whiteSpace_tree_constructors;
	public static String FormatterModifyDialog_whiteSpace_tree_declarations;
	public static String FormatterModifyDialog_whiteSpace_tree_do;
	public static String FormatterModifyDialog_whiteSpace_tree_enums;
	public static String FormatterModifyDialog_whiteSpace_tree_expressions;
	public static String FormatterModifyDialog_whiteSpace_tree_fields;
	public static String FormatterModifyDialog_whiteSpace_tree_for;
	public static String FormatterModifyDialog_whiteSpace_tree_if;
	public static String FormatterModifyDialog_whiteSpace_tree_labels;
	public static String FormatterModifyDialog_whiteSpace_tree_lambda;
	public static String FormatterModifyDialog_whiteSpace_tree_localvars;
	public static String FormatterModifyDialog_whiteSpace_tree_methods;
	public static String FormatterModifyDialog_whiteSpace_tree_operators;
	public static String FormatterModifyDialog_whiteSpace_tree_param_type_ref;
	public static String FormatterModifyDialog_whiteSpace_tree_parameterized_types;
	public static String FormatterModifyDialog_whiteSpace_tree_parenexpr;
	public static String FormatterModifyDialog_whiteSpace_tree_return;
	public static String FormatterModifyDialog_whiteSpace_tree_statements;
	public static String FormatterModifyDialog_whiteSpace_tree_switch;
	public static String FormatterModifyDialog_whiteSpace_tree_synchronized;
	public static String FormatterModifyDialog_whiteSpace_tree_throw;
	public static String FormatterModifyDialog_whiteSpace_tree_try_with_resources;
	public static String FormatterModifyDialog_whiteSpace_tree_type_arguments;
	public static String FormatterModifyDialog_whiteSpace_tree_type_parameters;
	public static String FormatterModifyDialog_whiteSpace_tree_typecasts;
	public static String FormatterModifyDialog_whiteSpace_tree_whitespace;
	public static String FormatterModifyDialog_whiteSpace_tree_wildcardtype;
	public static String ModifyDialog_BuiltIn_Status;
	public static String ModifyDialog_Duplicate_Status;
	public static String ModifyDialog_EmptyName_Status;
	public static String ModifyDialog_Export_Button;
	public static String ModifyDialog_NewCreated_Status;
	public static String ModifyDialog_ProfileName_Label;
	public static String ModifyDialog_Shared_Status;
	public static String ProfileConfigurationBlock_load_profile_wrong_profile_message;
	public static String CodingStyleConfigurationBlock_preview_title;

	public static String ProfileManager_default_profile_name;
	public static String ProfileManager_eclipse_profile_name;
	public static String ProfileManager_java_conventions_profile_name;

	public static String JavaPreview_formatter_exception;

	public static String AlreadyExistsDialog_message_profile_already_exists;
	public static String AlreadyExistsDialog_message_profile_name_empty;
	public static String AlreadyExistsDialog_dialog_title;
	public static String AlreadyExistsDialog_dialog_label;
	public static String AlreadyExistsDialog_rename_radio_button_desc;
	public static String AlreadyExistsDialog_overwrite_radio_button_desc;

	public static String CodingStyleConfigurationBlock_save_profile_dialog_title;
	public static String CodingStyleConfigurationBlock_save_profile_error_title;
	public static String CodingStyleConfigurationBlock_save_profile_error_message;
	public static String CodingStyleConfigurationBlock_load_profile_dialog_title;
	public static String CodingStyleConfigurationBlock_load_profile_error_title;
	public static String CodingStyleConfigurationBlock_load_profile_error_message;
	public static String CodingStyleConfigurationBlock_load_profile_error_too_new_title;
	public static String CodingStyleConfigurationBlock_load_profile_error_too_new_message;
	public static String CodingStyleConfigurationBlock_save_profile_overwrite_title;
	public static String CodingStyleConfigurationBlock_save_profile_overwrite_message;
	public static String CodingStyleConfigurationBlock_export_all_button_desc;
	public static String CodingStyleConfigurationBlock_export_profiles_dialog_title;
	public static String CodingStyleConfigurationBlock_export_profiles_error_title;
	public static String CodingStyleConfigurationBlock_export_profiles_error_message;
	public static String CodingStyleConfigurationBlock_export_profiles_overwrite_title;
	public static String CodingStyleConfigurationBlock_export_profiles_overwrite_message;
	public static String CodingStyleConfigurationBlock_edit_button_desc;
	public static String CodingStyleConfigurationBlock_remove_button_desc;
	public static String CodingStyleConfigurationBlock_new_button_desc;
	public static String CodingStyleConfigurationBlock_load_button_desc;
	public static String CodingStyleConfigurationBlock_preview_label_text;
	public static String CodingStyleConfigurationBlock_error_reading_xml_message;
	public static String CodingStyleConfigurationBlock_error_serializing_xml_message;
	public static String CodingStyleConfigurationBlock_delete_confirmation_title;
	public static String CodingStyleConfigurationBlock_delete_confirmation_question;

	public static String CustomCodeFormatterBlock_formatter_name;

	public static String CreateProfileDialog_status_message_profile_with_this_name_already_exists;
	public static String CreateProfileDialog_status_message_profile_name_is_empty;
	public static String CreateProfileDialog_dialog_title;
	public static String CreateProfileDialog_profile_name_label_text;
	public static String CreateProfileDialog_base_profile_label_text;
	public static String CreateProfileDialog_open_edit_dialog_checkbox_text;

	public static String ModifyDialog_dialog_title;
	public static String ModifyDialog_apply_button;
	public static String ModifyDialog_ComboPreference_error_invalid_key;
	public static String ModifyDialog_filter_hint;
	public static String ModifyDialog_filter_label;
	public static String ModifyDialog_modifyAll_checkBox;
	public static String ModifyDialog_modifyAll_summary;
	public static String ModifyDialog_modifyAll_tooltip;
	public static String ModifyDialog_previewMissing_comment;
	public static String ModifyDialogTabPage_preview_label_text;

	public static String ProfileManager_unmanaged_profile;
	public static String ProfileManager_unmanaged_profile_with_name;

	public static String ModifyDialogTabPage_error_msg_values_text_unassigned;
	public static String ModifyDialogTabPage_error_msg_values_items_text_unassigned;
	public static String ModifyDialogTabPage_NumberPreference_error_invalid_key;
	public static String ModifyDialogTabPage_NumberPreference_error_invalid_value;

	static {
		NLS.initializeMessages(BUNDLE_NAME, FormatterMessages.class);
	}
}
